package syxtus.time;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static syxtus.time.Time.TimeConfig.*;

@Mod(modid="time", name="Time", version = "1.0.0", acceptedMinecraftVersions = "1.16", acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber
public class Time {
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if ((event.phase == TickEvent.Phase.START) && (event.world.provider.getDimension() == 0)) {
            if(event.world.getWorldTime() % 24000 >= 1000 && event.world.getWorldTime() % 24000 < 13000) {
                if (longerDays) {
                    if (!(event.world.getTotalWorldTime() % dayTimeMultiplier == 0)) {
                        event.world.setWorldTime(event.world.getWorldTime() - 1);
                    }
                } else {
                    event.world.setWorldTime(event.world.getWorldTime() + (dayTimeMultiplier - 1));
                }
            } else{
                if (longerNights) {
                    if (!(event.world.getTotalWorldTime() % nightTimeMultiplier == 0)) {
                        event.world.setWorldTime(event.world.getWorldTime() - 1);
                    }
                } else {
                    event.world.setWorldTime(event.world.getWorldTime() + (nightTimeMultiplier - 1));
                }
            }
        }
    }
    @SubscribeEvent
    public static void configReload(ConfigChangedEvent.OnConfigChangedEvent event){
        if(event.getModID().equals("time")) ConfigManager.sync("time", Config.Type.INSTANCE);
    }
    @Config(modid="time")
    public static class TimeConfig{
        @Config.Name("Time")
        @Config.Comment("Whether to make days longer or shorter,\nif set to true days shall be longer (according to Day Time Multiplier),\nif set to false days shall be shorter (according to Day Time Multiplier)")
        public static boolean longerDays = true;
        @Config.Name("Day Time Multiplier")
        @Config.Comment("Rhe multiplier by which to make days longer or shorter")
        public static byte dayTimeMultiplier = 3;
        @Config.Name("Longer Nights")
        @Config.Comment("Whether to make night longer or shorter,\nif set to true nights shall be longer (according to Night Time Multiplier),\nif set to false nights shall be shorter (according to Night Time Multiplier)")
        public static boolean longerNights = true;
        @Config.Name("Night Time Multiplier")
        @Config.Comment("The multiplier by which to make nights longer or shorter")
        public static byte nightTimeMultiplier = 3;
    }
}
