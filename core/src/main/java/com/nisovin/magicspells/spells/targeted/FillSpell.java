package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.factory.SphereRegionFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FillSpell extends TargetedSpell implements TargetedLocationSpell {

    public boolean castAtCaster;
    public boolean fullSphere;
    public int maxFillBlocks;
    public List<String> toReplace;
    public List<String> replaceWith;
    public int radius;

    public FillSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        castAtCaster = getConfigBoolean("point-blank", false);
        fullSphere = getConfigBoolean("full-sphere", true);
        maxFillBlocks = getConfigInt("max-fill-blocks", -1);

        List<String> defaultToReplaceList  = new ArrayList<>(List.of("air"));
        List<String> defaultToPlaceList  = new ArrayList<>(List.of("dirt"));

        toReplace = getConfigStringList("to-replace", defaultToReplaceList);
        replaceWith = getConfigStringList("replace-with", defaultToPlaceList);

        radius = getConfigInt("radius", 5);
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if(castAtCaster){
            fillZone(caster, caster.getLocation());
        }else{
            fillZone(caster, getTargetedBlock(caster, power, args).getLocation());
        }
        return PostCastAction.ALREADY_HANDLED;
    }

    @Override
    public boolean castAtLocation(LivingEntity caster, Location target, float power) {
        fillZone(caster, target);
        return castAtLocation(target, power);
    }

    @Override
    public boolean castAtLocation(Location target, float power) {
        return false;
    }

    private void fillZone(LivingEntity caster, Location location){
        FillHandler _holder = new FillHandler(caster, location);
        scheduleDelayedTask(_holder::start, 0);
    }

    private class FillHandler {
        Region region;
        World bWorld;

        Vector3 centerPoint;
        BlockVector3 lowestCenter;

        Random randomizer;

        int totalPlaced = 0;

        List<BlockVector3> replaceLocations;

        LivingEntity caster;

        Location startLocation;

        public FillHandler(LivingEntity caster, Location location){
            SphereRegionFactory sphereBuilder = new SphereRegionFactory();
            region = sphereBuilder.createCenteredAt(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()), radius);
            this.caster = caster;
            bWorld = caster.getWorld();
            centerPoint = region.getCenter();

            startLocation = location;

            replaceLocations = new ArrayList<>();

            int lowestY = region.getMinimumPoint().getY();
            lowestCenter = BlockVector3.at(centerPoint.getX(), lowestY, centerPoint.getZ());

            randomizer = new Random();
            loadBlock(lowestCenter, 0);
        }

        private void loadBlock(BlockVector3 point, int layer){
            //Location location = new Location(bWorld, startPosition.getX(), startPosition.getY(), startPosition.getZ());

            CuboidRegion cuboidRegion = region.getBoundingBox();
            Iterable<BlockVector2> layerZone = cuboidRegion.asFlatRegion();

            for(BlockVector2 vec2 : layerZone){
                BlockVector3 pos = BlockVector3.at(vec2.getX(), point.getY(), vec2.getZ());
                if(region.contains(pos))
                    replaceLocations.add(pos);
            }

            if(layer < region.getHeight()){
                if(!fullSphere && point.add(0,1,0).getY() >= startLocation.getY()) {
                    return;
                }
                loadBlock(point.add(0, 1, 0), layer+1);
            }
        }


        public void start(){
            BlockVector3 placePosition = getNextBlock(lowestCenter);
            placeBlock(placePosition);
        }

        private BlockVector3 getNextBlock(BlockVector3 centerPoint){
            double closestDistance = Float.MAX_VALUE;
            BlockVector3 current = null;
            List<BlockVector3> cleanup = new ArrayList<>();
            for(BlockVector3 pos : replaceLocations){
                if(pos.getY() > centerPoint.getY()) break;
                if(toReplace.stream().noneMatch(e -> e.equalsIgnoreCase(bWorld.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getType().toString()))){
                    cleanup.add(pos);
                    continue;
                }
                double blockDist = pos.distance(centerPoint);
                if(blockDist < closestDistance){
                    current = pos;
                    closestDistance = blockDist;
                }
            }
            replaceLocations.removeAll(cleanup);
            return current;
        }

        private void placeBlock(BlockVector3 pos){
            if(pos == null && replaceLocations.size() != 0){
                //If for some reason we get a null value on first pass then we should try again on the next layer
                lowestCenter = lowestCenter.add(0, 1, 0);
                placeBlock(getNextBlock(lowestCenter));
                return;
            } else if(pos == null){
                //This shouldn't be possible but apparently it is
                return;
            }

            Material toPlace = Material.valueOf(replaceWith.get(randomizer.nextInt(0, replaceWith.size())).toUpperCase());

            bWorld.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).setType(toPlace);
            totalPlaced++;

            replaceLocations.remove(pos);
            BlockVector3 point = getNextBlock(lowestCenter);
            if(point == null && replaceLocations.size() != 0){
                lowestCenter = lowestCenter.add(0, 1, 0);
                point = getNextBlock(lowestCenter);
            } else if(point == null)
                return;
            if(!(maxFillBlocks != -1 && totalPlaced >= maxFillBlocks)){
                placeBlock(point);
            }
        }
    }
}
