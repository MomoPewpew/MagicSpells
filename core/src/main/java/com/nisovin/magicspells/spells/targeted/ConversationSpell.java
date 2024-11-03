package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.util.SpellData;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.prompt.ConversationContextUtil;

public class ConversationSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConversationFactory conversationFactory;

	public ConversationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		conversationFactory = ConfigReaderUtil.readConversationFactory(getConfigSection("conversation"));
	}

	@Override
	public PostCastAction castSpell(SpellCastState state, SpellData data) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(data);
			if (targetInfo.noTarget()) return noTarget(data, targetInfo);

			conversate(data.builder().target(targetInfo.target()).power(targetInfo.getPower()).build());
			sendMessages(data.caster(), targetInfo.target(), data.args());

			return PostCastAction.NO_MESSAGES;
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(SpellData data) {
		if (!validTargetList.canTarget(data.caster(), data.target()) || !(data.target() instanceof Player)) return false;
		conversate(data);
		return true;
	}

	private void conversate(SpellData data) {
		LivingEntity caster = data.caster();
		Conversable target = (Conversable) data.target();

		Conversation conversation = conversationFactory.buildConversation(target);
		ConversationContextUtil.setConversable(conversation.getContext(), target);
		conversation.begin();

		if (caster != null) playSpellEffects(data);
		else playSpellEffects(EffectPosition.TARGET, caster, data);
	}

}
