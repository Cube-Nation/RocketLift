package de.derflash.plugins.rocketlift;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class RocketLift extends JavaPlugin implements Listener {
	
	private ConfigurationSection liftsSave;
	private HashMap<Player, Integer> admins = new HashMap<Player, Integer>();
	private HashMap<Player, Long> lastUses = new HashMap<Player, Long>();
	
	private int defaultCooldown;
	private int defaultTimer;
	private Vector defaultVelocity;
	
    public void onDisable() {
    	this.saveList();
    }
    
    public void saveList() {
    	System.out.println("Saved locations: " + this.getConfig().getConfigurationSection("rocketLifts"));
    	
    	this.saveConfig();
    }
    
    public void onEnable() {
    	liftsSave = this.getConfig().getConfigurationSection("rocketLifts");
    	
    	defaultTimer = getConfig().getInt("timer", 2);
    	defaultCooldown = getConfig().getInt("cooldown", 5);
    	defaultVelocity = getConfig().getVector("velocity", new Vector(0,2,0));

    	if (liftsSave == null) {
    		liftsSave = new MemoryConfiguration();
    		this.getConfig().set("rocketLifts",	liftsSave);
    		this.getLogger().info("Created new section: rocketLifts");
    	}
    	
    	System.out.println("Loaded locations: " + liftsSave);
    	
        getServer().getPluginManager().registerEvents(this, this);
        
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
			public void run() {
				ArrayList<Player> toRemove = new ArrayList<Player>();
				long now = new Date().getTime();
				
				for (Player player : lastUses.keySet()) {
					if (!player.isOnline()) toRemove.add(player);
					if (now - lastUses.get(player) > cooldown * 1000) toRemove.add(player);
				}
				
				for (Player player : toRemove) {
					lastUses.remove(player);
				}
				
			}}, 1200L, 1200L);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        final Player player = event.getPlayer();

        if(event.getAction().equals(Action.PHYSICAL)){
        	Block block = event.getClickedBlock();

            if(block.getTypeId() == 70) {
            	String worldName = player.getWorld().getName();
            	
            	if (admins.containsKey(player)) {
                	int mode = admins.get(event.getPlayer());
            		admins.remove(player);
            		
            		@SuppressWarnings("unchecked")
					List<Vector> locations = (List<Vector>) liftsSave.getList(worldName);
            		if (locations == null) {
            			locations = new ArrayList<Vector>();
            			liftsSave.set(worldName, locations);
            		}
            		
                	switch (mode) {

                	case 0: {
                		
                		if (locations.add(event.getClickedBlock().getLocation().toVector())) {
                			player.sendMessage("Pressure plate added!");
                		} else {
                			player.sendMessage("Could not add pressure plate! Console?!");
                		}
                		
                	} break;

                	case 1: {
                		admins.remove(player);
                		if (locations.remove(event.getClickedBlock().getLocation().toVector())) {
                			player.sendMessage("Pressure plate removed!");
                		} else {
                			player.sendMessage("Could not find pressure plate within database for this location!");
                		}
                		
                	} break;

                	}
                	
            	} else if (liftsSave.getList(worldName) != null && liftsSave.getList(worldName).contains(block.getLocation().toVector())) {
            		
            		if (lastUses.containsKey(player)) {
            			Long lastUse = lastUses.get(player);
            			if (new Date().getTime() - lastUse.longValue() < cooldown * 1000) {
                			player.sendMessage("Cooldown aktiv. Versuch es in wenigen Sekunden noch einmal.");
            				return;
            			}
            		}
            		
            		final Location oldLocation = player.getLocation();
                    this.getServer().getScheduler().runTaskLater(this, new Runnable() {
						public void run() {
							
							if (player.isOnline()) {
			            		
								if (player.getLocation().distance(oldLocation) < 1) {
				            		lastUses.put(player, new Date().getTime());
				            		spawnFirework(player.getLocation());
				                    player.setVelocity(velocity);
								}
							}
							
						}}, timer * 20L);
                    
            	}

            }
        }
    }
    
    protected void spawnFirework(Location location) {
        List<Location> smokeLocations = new ArrayList<Location>();
        smokeLocations.add(location);
        smokeLocations.add(location.clone().add(0, 1, 0));
        SmokeUtil.spawnCloudRandom(smokeLocations, 3);
        
        Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();
              
        fwm.addEffect(FireworkEffect.builder().withColor(Color.BLACK).with(Type.BURST).build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);           
		
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	Player player = (Player)sender;
    	
		if (!player.hasPermission("rocketlift.admin")) {
			player.sendMessage("You are not allowed to do that!");
			return true;
		}
		
    	if(cmd.getName().equalsIgnoreCase("rocketlift")){
    		
    		if (args.length > 0) {
    			
    			if (args[0].equalsIgnoreCase("add")) {
    				
					for (int i = 0; i < args.length; i++) {
						
					}
    				
    				admins.put(player, 0);
    				player.sendMessage("Now use the pressure plate you want to add!");
    				
    			} else if (args[0].equalsIgnoreCase("remove")) {
    				admins.put(player, 1);
    				player.sendMessage("Now use the pressure plate you want to remove!");

    			} else if (args[0].equalsIgnoreCase("timer")) {
    				if (args.length == 2) {
        				timer = Integer.parseInt(args[1]);
        				getConfig().set("timer", timer);
        				
        				player.sendMessage("Timer set to: " + timer);
    				} else {
        				player.sendMessage("Timer: " + timer);
    				}
    				

    			} else if (args[0].equalsIgnoreCase("cooldown")) {
    				if (args.length == 2) {
        				cooldown = Integer.parseInt(args[1]);
        				getConfig().set("cooldown", cooldown);
        				
        				player.sendMessage("Cooldown set to: " + cooldown);
    				} else {
        				player.sendMessage("Cooldown: " + cooldown);

    				}
    				
    			} else if (args[0].equalsIgnoreCase("velocity")) {
    				if (args.length == 4) {
        				float x = Float.parseFloat(args[1]);
        				float y = Float.parseFloat(args[2]);
        				float z = Float.parseFloat(args[3]);
        				
        				velocity = new Vector(x,y,z);
        				getConfig().set("velocity", velocity);
        				
        				player.sendMessage("Velocity set to: " + velocity);
    				} else {
        				player.sendMessage("Velocity: " + velocity.toString());

    				}
    				
    			} else if (args[0].equalsIgnoreCase("save")) {
    				this.saveList();
    				
    			}
    			
    		}
    		return true;
    	}
    	
    	return false; 
    }
	
	public class RocketModification {

	}
	
	public class RocketModificationAdd extends RocketModification {
		public int cooldown;
		public int timer;
		public Vector velocity;
		
		RocketModificationAdd(int cooldown, int timer, Vector velocity) {
			this.cooldown = cooldown;
			this.timer = timer;
			this.velocity = velocity;
		}
	}

	public class RocketModificationRemove extends RocketModification {
		
	}

    
}

