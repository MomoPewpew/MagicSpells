package com.nisovin.magicspells.castmodifiers.conditions;
//By Mami
// This is the complete abstract syntax for an nbt expression:
// <expr> ::= <and>
// <and> ::= <or>&<or>; <or>
// <or> ::= <not>|<not>; <not>
// <not> ::= !<not>; (<and>); <op>
// <op> ::= <comp>; <exists>
// <comp> ::= <key>=<string>; <key>=<numeral>; <key>><numeral>; <key><<numeral>
// <key> ::= <string>.<key>; <string>[<numeral>]
// <exists> ::= <string>.<exists>; <string>[<numeral>]; <string>[<and>]
// <numeral> can be any string that can parse as a double in java
// <string> can be any string with backslash escapes for symbols (\&, \., \=, \\, etc.)
// DO NOT PUT WHITESPACE CHARACTERS OR TICK MARKS (' or ") INTO AN NBT EXPR, THIS WILL BREAK MAGICSPELLS PARSING EVEN IF YOU ESCAPE THEM

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//THESE IMPORTS HERE ARE VOLATILE, THE CORRECT VERSION OF MINECRAFT MUST BE USED
//currently configured only for 1.12 mc
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import net.minecraft.server.v1_12_R1.NBTBase;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.EntityLiving;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.Objects;
import java.util.ArrayList;

public class NBTExpr extends Condition {

	public enum ASTType {
		EXISTS, GT, LT, EQ, STREQ,//nbt operation types
		KEY, INDEX, FIND,//nbt indexing types
		AND, OR, NOT//expression operation types
	}
	public class NBTExpr_AST {
		public ASTType type;
		public String str;
		public double numeral;
		public NBTExpr_AST child0;
		public NBTExpr_AST child1;
	}
	public NBTExpr_AST config_ast;

	public interface ParseFunc {
		public NBTExpr_AST func(ArrayList<String> tokens, int[] token_i, String[] error_msg);
	}

	//All of the lexing and parsing code here was cannibalised from my own lexing and parsing code from an old project
	ArrayList<String> lex(String source, String[] error_msg) {
		//incredibly lazy lexing, hopefully this does not cause issues in the future
		//the proper way to do this is to track a DFA state while lexing and then store that state information with each token
		ArrayList<String> tokens = new ArrayList<String>();
		int i = 0;
		String word = null;
		while(i < source.length()) {
			char ch = source.charAt(i);
			if (ch == '\\') {
				i += 1;
				if(i >= source.length()) break;
				ch = source.charAt(i);
				if(word == null) {
					word = "_" + ch;
				} else {
					word += ch;
				}
			} else if (ch == '.' || ch == '[' || ch == ']' || ch == '(' || ch == ')' || ch == '=' || ch == '<' || ch == '>' || ch == '&' || ch == '|' || ch == '!') {
				if(word != null) {
					tokens.add(word);
					word = null;
				}
				tokens.add("" + ch);
			} else if(ch == ' ' || ch == '\t' || ch == '\n') {
				if(word != null) {
					tokens.add(word);
					word = null;
				}
			} else if('0' <= ch && ch <= '9') {
				//lazy implementation of numeral lexing, the proper way to do this is by tracking a DFA state while lexing
				String num = "";
				while(i < source.length()) {
					ch = source.charAt(i);
					if(('0' <= ch && ch <= '9') || ch == '.') {
						num = num + ch;
						i += 1;
					} else {
						i -= 1;
						break;
					}
				}
				if(word != null) {
					tokens.add(word);
					word = null;
				}
				tokens.add(num);
			} else if(word == null) {
				word = "_" + ch;//all nbt keys are marked with _ prefixes, this is lazy, better to store a DFA state with the token
			} else {
				word += ch;
			}
			i += 1;
		}
		if (word != null) {
			tokens.add(word);
			word = null;
		}
		tokens.add("\n");//marks end of expr
		return tokens;
	}

	String msg_unexpected(String unexpected, String expected) {
		if(unexpected.equals("\n")) {
			unexpected = "End of Expression";
		} else if(unexpected.charAt(0) == '_') {
			unexpected = unexpected.substring(1, unexpected.length());
		}
		return "NBTExpr syntax error: Unexpected token, expected " + expected + "; got " + unexpected;
	}

	boolean eat_token(ArrayList<String> tokens, int[] token_i, String str) {
		if(str.equals(tokens.get(token_i[0]))) {
			token_i[0] += 1;
			return true;
		} else {
			return false;
		}
	}

	NBTExpr_AST parse_left_binops_loop_(NBTExpr_AST left_node, ArrayList<String> tokens, int[] token_i, String[] error_msg, String[] op_strings, ASTType[] op_types, ParseFunc parse_lower) {
		int binop_i = -1;
		for(int i = 0; i < op_strings.length; i++) {
			if(eat_token(tokens, token_i, op_strings[i])) {
				binop_i = i;
				break;
			}
		}
		if(binop_i < 0) return left_node;

		NBTExpr_AST right_node = parse_lower.func(tokens, token_i, error_msg);
		if(right_node == null) return null;
		NBTExpr_AST new_node = new NBTExpr_AST();
		new_node.type = op_types[binop_i];
		new_node.child0 = left_node;
		new_node.child1 = right_node;
		return parse_left_binops_loop_(new_node, tokens, token_i, error_msg, op_strings, op_types, parse_lower);
	}
	NBTExpr_AST parse_left_binops(ArrayList<String> tokens, int[] token_i, String[] error_msg, String[] op_strings, ASTType[] op_types, ParseFunc parse_lower) {
		NBTExpr_AST node = parse_lower.func(tokens, token_i, error_msg);
		if(node == null) return null;
		return parse_left_binops_loop_(node, tokens, token_i, error_msg, op_strings, op_types, parse_lower);
	}



	NBTExpr_AST parse_key(ArrayList<String> tokens, int[] token_i, String[] error_msg, boolean parent_is_comp) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: key:" + parent_is_comp + " " + tokens.get(token_i[0]));
		String cur_token = tokens.get(token_i[0]);
		if(cur_token.charAt(0) == '_') {
			NBTExpr_AST node = new NBTExpr_AST();
			node.type = ASTType.KEY;
			node.str = cur_token.substring(1, cur_token.length());
			token_i[0] += 1;
			if(eat_token(tokens, token_i, ".")) {
				node.child0 = parse_key(tokens, token_i, error_msg, parent_is_comp);
				if(node.child0 == null) return null;
				return node;
			} else if(eat_token(tokens, token_i, "[")) {
				try {
					//note: this is a lazy way of implementing numerical indexing: "list[5]", the proper way to do this is with a non-deterministic call to a recursive parsing function. As a consequence, multi-dimensional arrays are not index-able, so "list[5][2]" does not work.
					node.child0 = new NBTExpr_AST();
					node.child0.type = ASTType.INDEX;
					node.child0.numeral = Double.parseDouble(tokens.get(token_i[0]));
					token_i[0] += 1;
					if(eat_token(tokens, token_i, "]")) {
						if(eat_token(tokens, token_i, ".")) {
							node.child0.child0 = parse_key(tokens, token_i, error_msg, parent_is_comp);
							if(node.child0.child0 == null) return null;
							return node;
						} else {
							return node;
						}
					} else {
						error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "closing \"]\"");
						return null;
					}
				} catch (Exception e) {
					if(parent_is_comp) {//block parsing a FIND expr when parent is a comp operator
						error_msg[0] = msg_unexpected(cur_token, "a numeral");
						return null;
					} else {
						node.child0 = new NBTExpr_AST();
						node.child0.type = ASTType.FIND;
						node.child0.child0 = parse_and(tokens, token_i, error_msg);
						if(node.child0.child0 == null) return null;
						if(eat_token(tokens, token_i, "]")) {
							return node;
						} else {
							error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "closing \"]\"");
							return null;
						}
					}
				}
			} else {
				return node;
			}
		} else {
			error_msg[0] = msg_unexpected(cur_token, "an nbt key");
			return null;
		}
	}

	NBTExpr_AST parse_comp(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: comp " + tokens.get(token_i[0]));
		NBTExpr_AST node_child = parse_key(tokens, token_i, error_msg, true);
		if(node_child == null) return null;
		if(eat_token(tokens, token_i, "<")) {
			NBTExpr_AST node = new NBTExpr_AST();
			node.type = ASTType.LT;
			node.child0 = node_child;
			try {
				node.numeral = Double.parseDouble(tokens.get(token_i[0]));
				token_i[0] += 1;
				return node;
			} catch (Exception e) {
				error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "numeral");
				return null;
			}
		} else if(eat_token(tokens, token_i, ">")) {
			NBTExpr_AST node = new NBTExpr_AST();
			node.type = ASTType.GT;
			node.child0 = node_child;
			try {
				node.numeral = Double.parseDouble(tokens.get(token_i[0]));
				token_i[0] += 1;
				return node;
			} catch (Exception e) {
				error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "numeral");
				return null;
			}
		} else if(eat_token(tokens, token_i, "=")) {
			NBTExpr_AST node = new NBTExpr_AST();
			node.child0 = node_child;
			String cur_token = tokens.get(token_i[0]);
			char ch = cur_token.charAt(0);
			if(ch == '_') {
				token_i[0] += 1;
				node.type = ASTType.STREQ;
				node.str = cur_token.substring(1, cur_token.length());
				return node;
			} else {
				node.type = ASTType.EQ;
				try {
					// MagicSpells.sendDebugMessage("NBTExpr debug: comp2 " + token_i[0] + " " + cur_token);
					token_i[0] += 1;
					node.numeral = Double.parseDouble(cur_token);
					return node;
				} catch (Exception e) {
					error_msg[0] = msg_unexpected(cur_token, "numeral or string");
					return null;
				}
			}
		} else {
			error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "comparison operator");
			return null;
		}
	}

	NBTExpr_AST parse_nbt_operation(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: op " + tokens.get(token_i[0]));
		//we need to non-deterministically test for a comparison operation separately from a EXISTS operation to prevent a FIND node from appearing as a child to a comparison operation
		//FIND is only interprettable as an nbt indexing operation when it is a child to an EXISTS operation
		int pre_token_i = token_i[0];
		String[] nd_error_msg = {""};
		NBTExpr_AST node = parse_comp(tokens, token_i, nd_error_msg);
		if(node == null) {
			//restore parser state to before non-det check
			token_i[0] = pre_token_i;
			// MagicSpells.sendDebugMessage("NBTExpr debug: op2 " + tokens.get(token_i[0]));
			node = new NBTExpr_AST();
			node.type = ASTType.EXISTS;
			node.child0 = parse_key(tokens, token_i, error_msg, false);
			if(node.child0 == null) {
				error_msg[0] = nd_error_msg[0];//the parse_comp error message is more likely to be informative, the proper way to decide which error message gets displayed is to build some kind of error priority system
				return null;
			}
			return node;
		} else {
			return node;
		}
	}

	NBTExpr_AST parse_not(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: not " + tokens.get(token_i[0]));
		if(eat_token(tokens, token_i, "!")) {
			NBTExpr_AST node = new NBTExpr_AST();
			node.type = ASTType.NOT;
			node.child0 = parse_not(tokens, token_i, error_msg);
			if(node.child0 == null) return null;
			return node;
		} else if(eat_token(tokens, token_i, "(")) {
			NBTExpr_AST node = parse_and(tokens, token_i, error_msg);
			if(node == null) return null;
			// MagicSpells.sendDebugMessage("NBTExpr debug: not2 " + tokens.get(token_i[0]));
			if(eat_token(tokens, token_i, ")")) {
				return node;
			} else {
				error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "closing \")\"");
				return null;
			}
		} else {
			return parse_nbt_operation(tokens, token_i, error_msg);
		}
	}
	NBTExpr_AST parse_or(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: or " + tokens.get(token_i[0]));
		String[] op_strings = {"|"};
		ASTType[] op_types   = {ASTType.OR};
		return parse_left_binops(tokens, token_i, error_msg, op_strings, op_types, new ParseFunc() {
			public NBTExpr_AST func(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
				return parse_not(tokens, token_i, error_msg);
			}
		});
	}
	NBTExpr_AST parse_and(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
		// MagicSpells.sendDebugMessage("NBTExpr debug: and " + tokens.get(token_i[0]));
		String[] op_strings = {"&"};
		ASTType[] op_types   = {ASTType.AND};
		return parse_left_binops(tokens, token_i, error_msg, op_strings, op_types, new ParseFunc() {
			public NBTExpr_AST func(ArrayList<String> tokens, int[] token_i, String[] error_msg) {
				return parse_or(tokens, token_i, error_msg);
			}
		});
	}

	NBTExpr_AST parse_expr(ArrayList<String> tokens, String[] error_msg) {
		int[] token_i = {0};
		NBTExpr_AST root = parse_and(tokens, token_i, error_msg);
		if(root == null) {
			return null;
		} else {
			if(eat_token(tokens, token_i, "\n")) {
				return root;
			} else {
				error_msg[0] = msg_unexpected(tokens.get(token_i[0]), "end of expression");
				return null;
			}
		}
	}





	public boolean eval_nbt_index(NBTExpr_AST root, NBTTagList taglist, NBTExpr_AST operation) {
		int i = (int)Math.floor(root.numeral);
		if(root.child0 != null) {
			return eval_nbt_key(root.child0, taglist.get(i), operation);
		} else if (operation.type == ASTType.EXISTS) {
			return 0 <= i && i < taglist.size();
		} else if (operation.type == ASTType.GT) {
			double test = taglist.c(i);
			return test > operation.numeral;
		} else if (operation.type == ASTType.LT) {
			double test = taglist.c(i);
			return test < operation.numeral;
		} else if (operation.type == ASTType.EQ) {
			double test = taglist.c(i);
			return Math.abs(test - operation.numeral) < .00001;
		} else if (operation.type == ASTType.STREQ) {
			String test = taglist.getString(i);
			return test.equals(operation.str);
		} else {
			MagicSpells.error("NBTExpr error: Invalid operation");
			return false;
		}
	}

	public boolean eval_nbt_key(NBTExpr_AST root, NBTTagCompound compound, NBTExpr_AST operation) {
		if(root.child0 != null) {
			if(root.child0.type == ASTType.INDEX) {
				NBTTagList taglist = compound.getList(root.str, 0);
				for(int i = 1; i < 12; i += 1) {//I hate this nbt interface
					taglist = compound.getList(root.str, i);
					if(taglist.size() > 0) break;
				}
				return eval_nbt_index(root.child0, taglist, operation);
			} else if(root.child0.type == ASTType.FIND) {
				if(operation.type == ASTType.EXISTS) {
					NBTTagList taglist = compound.getList(root.str, 10);
					for(int i = 0; i < taglist.size(); i += 1) {
						NBTTagCompound cur_compound = taglist.get(i);
						if(eval_expr(root.child0.child0, cur_compound)) {
							return true;
						}
					}
					return false;
				} else {
					//should not be possible
					MagicSpells.error("NBTExpr error: Malformed syntax tree, expected operation type EXISTS");
					return false;
				}
			} else {
				return eval_nbt_key(root.child0, compound.getCompound(root.str), operation);
			}
		} else if (operation.type == ASTType.EXISTS) {
			return compound.hasKey(root.str);
		} else if (operation.type == ASTType.GT) {
			double test = compound.getDouble(root.str);
			return test > operation.numeral;
		} else if (operation.type == ASTType.LT) {
			double test = compound.getDouble(root.str);
			return test < operation.numeral;
		} else if (operation.type == ASTType.EQ) {
			double test = compound.getDouble(root.str);
			return Math.abs(test - operation.numeral) < .00001;
		} else if (operation.type == ASTType.STREQ) {
			String test = compound.getString(root.str);
			return test.equals(operation.str);
		} else {
			//should not be possible
			MagicSpells.error("NBTExpr error: Invalid operation");
			return false;
		}
	}
	public boolean eval_expr(NBTExpr_AST root, NBTTagCompound compound) {
		if (root.type == ASTType.EXISTS || root.type == ASTType.GT || root.type == ASTType.LT || root.type == ASTType.EQ || root.type == ASTType.STREQ) {
			if(root.child0.type == ASTType.KEY) {
				return eval_nbt_key(root.child0, compound, root);
			} else {
				//should not be possible
				MagicSpells.error("NBTExpr error: Malformed syntax tree, expected child type KEY");
				return false;
			}
		} else if (root.type == ASTType.AND) {
			return eval_expr(root.child0, compound) && eval_expr(root.child1, compound);
		} else if (root.type == ASTType.OR) {
			return eval_expr(root.child0, compound) || eval_expr(root.child1, compound);
		} else if (root.type == ASTType.NOT) {
			return !(eval_expr(root.child0, compound));
		} else {
			MagicSpells.error("NBTExpr error: Malformed syntax tree, expected operation type");
			//This error should not be possible given a syntax tree produced by lexing and parsing
			//The only remaining ast types; KEY, INDEX, FIND, require a parent with a nbt operation type to be interpretted
			//If anyone wants to run eval_expr on a syntax tree not produced by the function parse_expr(), be careful when constructing the tree or else errors like this may be produced.
			return false;
		}
	}



	@Override
	public boolean setVar(String var) {
		String[] error_msg = {""};
		ArrayList<String> tokens = lex(var, error_msg);
		// MagicSpells.sendDebugMessage("NBTExpr debug: " + tokens);
		if(tokens == null) {
			MagicSpells.error(error_msg[0]);
			return false;
		}
		config_ast = parse_expr(tokens, error_msg);
		if(config_ast == null) {
			MagicSpells.error(error_msg[0]);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean check(Player player) {
		return check((LivingEntity)player);
	}

	public boolean check(LivingEntity target) {
		NBTTagCompound compound = new NBTTagCompound();

		EntityLiving el = ((CraftLivingEntity)target).getHandle();

    	el.b(compound);

		return eval_expr(config_ast, compound);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
