
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;

public class mod_MyMiniMap extends BaseMod implements Runnable
{
	static public String settings_filename = "minimap.settings",
		colors_filename = "map_colors";
	public enum keys_indexes { ZOOM_IN, ZOOM_OUT, OPTIONS, WAYPOINTS };
	public class key_definition
	{
		keys_indexes key;
		int value;
		String label;

		public key_definition(keys_indexes key, String label, int value)
		{
			this.key = key;
			this.label = label;
			this.value = value;
		}
	}
	static public ArrayList<key_definition> keys_list = new ArrayList<key_definition>();

	public key_definition get_key(keys_indexes key)
	{
		Iterator<key_definition> key_item = keys_list.iterator();
		while (key_item.hasNext())
		{
			key_definition current = key_item.next(); 
			if (current.key == key)
				return current;
		}
		return null;
	}
	
	public mod_MyMiniMap()
	{
        active = false;
        last_dimension = 0;
        zoom = 1;
        last_x = 0;
        last_z = 0;
        direction = 0.0f;
        timer = 0;
        info_message = "";
        message_timer = 0;
        temp_counter = 0;
        instance = this;
        threading = false;
        texture = 0;
        engine = null;
        calc_z = new Thread(this);
        ModLoader.SetInGameHook(this, true, false);
        calc_z.start();
	}

	private void initialize_data()
	{
		randomize = new Random();
        map = new BufferedImage[4];
        map[0] = new BufferedImage(32, 32, 2);
        map[1] = new BufferedImage(64, 64, 2);
        map[2] = new BufferedImage(128, 128, 2);
        map[3] = new BufferedImage(256, 256, 2);
        the_entities = new ArrayList<other_entity>();
        initialize_menu();
        initialize_colors();
        engine.to_chat("My MiniMap " + Version());
        engine.to_chat("- Press §B" + Keyboard.getKeyName(get_key(keys_indexes.ZOOM_IN).value) + " §Fto zoom in, §B" + Keyboard.getKeyName(get_key(keys_indexes.ZOOM_OUT).value) + " §Fto zoom out, or §B" + Keyboard.getKeyName(get_key(keys_indexes.OPTIONS).value) + "§F for options.");
        the_waypoints = new waypoints();
	}
	
	private void initialize_menu()
	{
	    scrollbar_click = false;
	    scrollbar_start = 0;
	    scrollbar_offset = 0;
	    scrollbar_size = 67;
	    first_entry = 0;
	    
	    blink = 0;
	    last_key = 0;
		keys_list.add(new key_definition(keys_indexes.ZOOM_IN, "Zoom Key In", Keyboard.KEY_INSERT));
		keys_list.add(new key_definition(keys_indexes.ZOOM_OUT, "Zoom Key Out", Keyboard.KEY_DELETE));
		keys_list.add(new key_definition(keys_indexes.OPTIONS, "Options menu Key", Keyboard.KEY_O));
		keys_list.add(new key_definition(keys_indexes.WAYPOINTS, "Waypoints menu Key", Keyboard.KEY_P));

	    try
	    {
		    main_menu = new String[2];
		    the_options = new menu_options(engine, settings_filename);
	        the_options.set(menu_options.options_indexes.DISPLAY, true);
	        the_options.set(menu_options.options_indexes.COORDINATES, true);
	        the_options.set(menu_options.options_indexes.LIGHTING, false);
	        the_options.set(menu_options.options_indexes.HEIGHTMAP, true);
	        the_options.set(menu_options.options_indexes.CAVEMAP, false);
	        the_options.set(menu_options.options_indexes.NETHERPOINTS, false);
	        the_options.set(menu_options.options_indexes.PLAYERS, true);
	        the_options.set(menu_options.options_indexes.MOBS, true);
	        
	        file_settings main_settings = new file_settings(settings_filename);
	        for (key_definition key : keys_list)
	        {
	        	if (main_settings.has(key.label))
	        		key.value = Keyboard.getKeyIndex(main_settings.get(key.label));
	        }
	
	        if (the_options.is_active(menu_options.options_indexes.CAVEMAP) && !(the_options.is_active(menu_options.options_indexes.LIGHTING) ^ the_options.is_active(menu_options.options_indexes.HEIGHTMAP)))
	        {
	        	the_options.set(menu_options.options_indexes.LIGHTING, true);
	        	the_options.set(menu_options.options_indexes.HEIGHTMAP, false);
	        }
	        if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
	        {
	            zoom = 1;
	            info_message = "Cavemap zoom (2.0x)";
	        }
	    } catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	private void initialize_colors()
	{
        block_colors = new BlockColor[4096];
        for (int i = 0; i < block_colors.length; i++)
            block_colors[i] = null;

        try
        {
        	file_settings colors_settings = new file_settings(colors_filename);

            block_colors[block_color_ID(0, 0)] = new BlockColor(0xff00ff, 0, TintType.NONE); //air
            int wood = 0xbc9862; //reused colors
            int water = 0x3256ff;
            int lava = 0xd96514;
            block_colors[block_color_ID(1, 0)] = new BlockColor(0x686868, 0xff, TintType.NONE); //stone
            block_colors[block_color_ID(2, 0)] = new BlockColor(0x74b44a, 0xff, TintType.GRASS); //grass
            block_colors[block_color_ID(3, 0)] = new BlockColor(0x79553a, 0xff, TintType.NONE); //dirt
            block_colors[block_color_ID(4, 0)] = new BlockColor(0x959595, 0xff, TintType.NONE); //cobble
            block_colors[block_color_ID(5, 0)] = new BlockColor(wood, 0xff, TintType.NONE); //wood
            block_colors[block_color_ID(6, 0)] = new BlockColor(0xa2c978, 0x80, TintType.FOLIAGE); //sapling 1
            block_colors[block_color_ID(6, 1)] = new BlockColor(0xa2c978, 0x80, TintType.PINE);    //sapling 2
            block_colors[block_color_ID(6, 2)] = new BlockColor(0xa2c978, 0x80, TintType.BIRCH);   //sapling 3
            block_colors[block_color_ID(7, 0)] = new BlockColor(0x333333, 0xff, TintType.NONE); //bedrock
            block_colors[block_color_ID(8, 0)] = new BlockColor(water, 0xc0, TintType.NONE); //water
            block_colors[block_color_ID(9, 0)] = new BlockColor(water, 0xb0, TintType.NONE); //moving water
            block_colors[block_color_ID(10, 0)] = new BlockColor(lava, 0xff, TintType.NONE); //lava
            block_colors[block_color_ID(11, 0)] = new BlockColor(lava, 0xff, TintType.NONE); //moving lava
            block_colors[block_color_ID(12, 0)] = new BlockColor(0xddd7a0, 0xff, TintType.NONE); //sand
            block_colors[block_color_ID(13, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //gravel
            block_colors[block_color_ID(14, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //gold ore
            block_colors[block_color_ID(15, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //iron ore
            block_colors[block_color_ID(16, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //coal ore
            
            block_colors[block_color_ID(17, 0)] = new BlockColor(0x675132, 0xff, TintType.NONE); //log 1
            block_colors[block_color_ID(17, 1)] = new BlockColor(0x342919, 0xff, TintType.NONE); //log 2
            block_colors[block_color_ID(17, 2)] = new BlockColor(0xc8c29f, 0xff, TintType.NONE); //log 3
            
            block_colors[block_color_ID(18, 0)] = new BlockColor(0x164d0c, 0xa0, TintType.NONE); //leaf
            block_colors[block_color_ID(19, 0)] = new BlockColor(0xe5e54e, 0xff, TintType.NONE); //sponge
            block_colors[block_color_ID(20, 0)] = new BlockColor(0xffffff, 0x80, TintType.NONE); //glass
            block_colors[block_color_ID(21, 0)] = new BlockColor(0x6d7484, 0xff, TintType.NONE); //lapis ore
            block_colors[block_color_ID(22, 0)] = new BlockColor(0x1542b2, 0xff, TintType.NONE); //lapis
            block_colors[block_color_ID(23, 0)] = new BlockColor(0x585858, 0xff, TintType.NONE); //dispenser
            block_colors[block_color_ID(24, 0)] = new BlockColor(0xc6bd6d, 0xff, TintType.NONE); //sandstone
            block_colors[block_color_ID(25, 0)] = new BlockColor(0x784f3a, 0xff, TintType.NONE); //noteblock
            block_colors[block_color_ID(26, 0)] = new BlockColor(0xa95d5d, 0xff, TintType.NONE); //bed
            
            //skip 27, 28, 30, 31, and 32 as they are all nonsolid and
            //notch's height map skips them
            
            block_colors[block_color_ID(35, 0)] = new BlockColor(0xe1e1e1, 0xff, TintType.NONE); //colored wool
            block_colors[block_color_ID(35, 1)] = new BlockColor(0xeb8138, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 2)] = new BlockColor(0xc04cca, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 3)] = new BlockColor(0x698cd5, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 4)] = new BlockColor(0xc5b81d, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 5)] = new BlockColor(0x3cbf30, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 6)] = new BlockColor(0xda859c, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 7)] = new BlockColor(0x434343, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 8)] = new BlockColor(0x9fa7a7, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 9)] = new BlockColor(0x277697, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 10)] = new BlockColor(0x7f33c1, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 11)] = new BlockColor(0x26339b, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 12)] = new BlockColor(0x57331c, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 13)] = new BlockColor(0x384e18, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 14)] = new BlockColor(0xa52d28, 0xff, TintType.NONE);
            block_colors[block_color_ID(35, 15)] = new BlockColor(0x1b1717, 0xff, TintType.NONE); //end colored wool
            
            block_colors[block_color_ID(37, 0)] = new BlockColor(0xf1f902, 0xff, TintType.NONE); //yellow flower
            block_colors[block_color_ID(38, 0)] = new BlockColor(0xf7070f, 0xff, TintType.NONE); //red flower
            block_colors[block_color_ID(39, 0)] = new BlockColor(0x916d55, 0xff, TintType.NONE); //brown mushroom
            block_colors[block_color_ID(40, 0)] = new BlockColor(0x9a171c, 0xff, TintType.NONE); //red mushroom
            block_colors[block_color_ID(41, 0)] = new BlockColor(0xfefb5d, 0xff, TintType.NONE); //gold block
            block_colors[block_color_ID(42, 0)] = new BlockColor(0xe9e9e9, 0xff, TintType.NONE); //iron block
            
            block_colors[block_color_ID(43, 0)] = new BlockColor(0xa8a8a8, 0xff, TintType.NONE); //double slabs
            block_colors[block_color_ID(43, 1)] = new BlockColor(0xe5ddaf, 0xff, TintType.NONE);
            block_colors[block_color_ID(43, 2)] = new BlockColor(0x94794a, 0xff, TintType.NONE);
            block_colors[block_color_ID(43, 3)] = new BlockColor(0x828282, 0xff, TintType.NONE);

            block_colors[block_color_ID(44, 0)] = new BlockColor(0xa8a8a8, 0xff, TintType.NONE); //single slabs
            block_colors[block_color_ID(44, 1)] = new BlockColor(0xe5ddaf, 0xff, TintType.NONE);
            block_colors[block_color_ID(44, 2)] = new BlockColor(0x94794a, 0xff, TintType.NONE);
            block_colors[block_color_ID(44, 3)] = new BlockColor(0x828282, 0xff, TintType.NONE);
            
            block_colors[block_color_ID(45, 0)] = new BlockColor(0xaa543b, 0xff, TintType.NONE); //brick
            block_colors[block_color_ID(46, 0)] = new BlockColor(0xdb441a, 0xff, TintType.NONE); //tnt
            block_colors[block_color_ID(47, 0)] = new BlockColor(0xb4905a, 0xff, TintType.NONE); //bookshelf
            block_colors[block_color_ID(48, 0)] = new BlockColor(0x1f471f, 0xff, TintType.NONE); //mossy cobble
            block_colors[block_color_ID(49, 0)] = new BlockColor(0x101018, 0xff, TintType.NONE); //obsidian
            block_colors[block_color_ID(50, 0)] = new BlockColor(0xffd800, 0xff, TintType.NONE); //torch
            block_colors[block_color_ID(51, 0)] = new BlockColor(0xc05a01, 0xff, TintType.NONE); //fire
            block_colors[block_color_ID(52, 0)] = new BlockColor(0x265f87, 0xff, TintType.NONE); //spawner
            block_colors[block_color_ID(53, 0)] = new BlockColor(wood, 0xff, TintType.NONE); //wood steps
            block_colors[block_color_ID(54, 0)] = new BlockColor(0x8f691d, 0xff, TintType.NONE); //chest
            block_colors[block_color_ID(55, 0)] = new BlockColor(0x480000, 0xff, TintType.NONE); //redstone wire
            block_colors[block_color_ID(56, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //diamond ore
            block_colors[block_color_ID(57, 0)] = new BlockColor(0x82e4e0, 0xff, TintType.NONE); //diamond block
            block_colors[block_color_ID(58, 0)] = new BlockColor(0xa26b3e, 0xff, TintType.NONE); //craft table
            block_colors[block_color_ID(59, 0)] = new BlockColor(0x00e210, 0xff, TintType.NONE); //crops
            block_colors[block_color_ID(60, 0)] = new BlockColor(0x633f24, 0xff, TintType.NONE); //cropland
            block_colors[block_color_ID(61, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //furnace
            block_colors[block_color_ID(62, 0)] = new BlockColor(0x808080, 0xff, TintType.NONE); //furnace, powered
            block_colors[block_color_ID(63, 0)] = new BlockColor(0xb4905a, 0xff, TintType.NONE); //fence
            block_colors[block_color_ID(64, 0)] = new BlockColor(0x7a5b2b, 0xff, TintType.NONE); //door
            block_colors[block_color_ID(65, 0)] = new BlockColor(0xac8852, 0xff, TintType.NONE); //ladder
            block_colors[block_color_ID(66, 0)] = new BlockColor(0xa4a4a4, 0xff, TintType.NONE); //track
            block_colors[block_color_ID(67, 0)] = new BlockColor(0x9e9e9e, 0xff, TintType.NONE); //cobble steps
            block_colors[block_color_ID(68, 0)] = new BlockColor(0x9f844d, 0xff, TintType.NONE); //sign
            block_colors[block_color_ID(69, 0)] = new BlockColor(0x695433, 0xff, TintType.NONE); //lever
            block_colors[block_color_ID(70, 0)] = new BlockColor(0x8f8f8f, 0xff, TintType.NONE); //stone pressureplate
            block_colors[block_color_ID(71, 0)] = new BlockColor(0xc1c1c1, 0xff, TintType.NONE); //iron door
            block_colors[block_color_ID(72, 0)] = new BlockColor(wood, 0xff, TintType.NONE); //wood pressureplate
            block_colors[block_color_ID(73, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //redstone ore
            block_colors[block_color_ID(74, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //glowing redstone ore
            block_colors[block_color_ID(75, 0)] = new BlockColor(0x290000, 0xff, TintType.NONE); //redstone torch, off
            block_colors[block_color_ID(76, 0)] = new BlockColor(0xfd0000, 0xff, TintType.NONE); //redstone torch, lit
            block_colors[block_color_ID(77, 0)] = new BlockColor(0x747474, 0xff, TintType.NONE); //button
            block_colors[block_color_ID(78, 0)] = new BlockColor(0xfbffff, 0xff, TintType.NONE); //snow
            block_colors[block_color_ID(79, 0)] = new BlockColor(0x8ebfff, 0xff, TintType.NONE); //ice
            block_colors[block_color_ID(80, 0)] = new BlockColor(0xffffff, 0xff, TintType.NONE); //snow block
            block_colors[block_color_ID(81, 0)] = new BlockColor(0x11801e, 0xff, TintType.NONE); //cactus
            block_colors[block_color_ID(82, 0)] = new BlockColor(0xbbbbcc, 0xff, TintType.NONE); //clay
            block_colors[block_color_ID(83, 0)] = new BlockColor(0xa1a7b2, 0xff, TintType.NONE); //reeds
            block_colors[block_color_ID(84, 0)] = new BlockColor(0xaadb74, 0xff, TintType.NONE); //record player
            block_colors[block_color_ID(85, 0)] = new BlockColor(wood, 0xff, TintType.NONE); //fence
            block_colors[block_color_ID(86, 0)] = new BlockColor(0xa25b0b, 0xff, TintType.NONE); //pumpkin
            block_colors[block_color_ID(87, 0)] = new BlockColor(0x582218, 0xff, TintType.NONE); //netherrack
            block_colors[block_color_ID(88, 0)] = new BlockColor(0x996731, 0xff, TintType.NONE); //slow sand
            block_colors[block_color_ID(89, 0)] = new BlockColor(0xcda838, 0xff, TintType.NONE); //glowstone
            block_colors[block_color_ID(90, 0)] = new BlockColor(0x732486, 0xff, TintType.NONE); //portal
            block_colors[block_color_ID(91, 0)] = new BlockColor(0xa25b0b, 0xff, TintType.NONE); //jackolantern

            Pattern colorline_id = Pattern.compile("^([0-9]*)\\.([0-9]*)$"),
            	colorline_value = Pattern.compile("^color=([0-9a-fA-F]*).alpha=([0-9a-fA-F]*) tint=(.*)$");

            Set<String> key_ids = colors_settings.keys();
            Iterator<String> key_id =  key_ids.iterator();
            while (key_id.hasNext())
            {
            	String key = key_id.next(),
            		value = colors_settings.get(key);
                Matcher match = colorline_id.matcher(key);
                if (match.matches())
                {
                    int id = Integer.parseInt(match.group(1));
                    int meta = Integer.parseInt(match.group(2));
                    
                    match = colorline_value.matcher(value);
                    int col = Integer.parseInt(match.group(1), 16);
                    int alpha = Integer.parseInt(match.group(2), 16);
                    TintType tint = TintType.get(match.group(3));
                    if (tint == null)
                    	tint = TintType.NONE;
                    block_colors[block_color_ID(id, meta)] = new BlockColor(col, alpha, tint);
                } else
                {
                    try
                    {
                        if (key.equals("Block"))
                        {
    	                    String[] values = value.split(":");
                            int newcol = Integer.parseInt(values[1], 16);
                            int id = Integer.parseInt(values[0]);
                            if (get_block_color(id, 0).color != newcol) // only act if it's not default
                                block_colors[block_color_ID(id, 0)] = new BlockColor(newcol, 0xff, TintType.NONE);
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            colors_settings.prepare_for_output();
            colors_settings.print("#Available tints: ");
            TintType[] availtints = TintType.values();
            for (int i = 0; i < availtints.length; i++)
            {
            	colors_settings.print(availtints[i].name());
                if (i < availtints.length - 1)
                	colors_settings.print(", ");
            }
            colors_settings.println();
            colors_settings.println("#format: blockid.metadata: color=RRGGBB/alpha=AA tint=TINTTYPE");
            for (int key = 1; key < block_colors.length; key++)
            {
                if (block_colors[key] == null)
                	continue;
                int meta = key >> 8;
                int id = key & 0xff;
                colors_settings.println("" + id + "." + meta + ": color=" + Integer.toHexString(block_colors[key].color) + "/alpha=" + Integer.toHexString(block_colors[key].alpha) + " tint=" + block_colors[key].tintType.name());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}


	private void save_main_settings()
    {
		try
		{
	    	the_options.save();
	    	file_settings main_settings = new file_settings(settings_filename);
	    	main_settings.prepare_for_output();
	        for (key_definition key : keys_list)
	        {
		    	main_settings.println(key.label + ":" + Keyboard.getKeyName(key.value));
	        }
		} catch (Exception e)
        {
            e.printStackTrace();
        }
    }
	
	public boolean OnTickInGame(Minecraft mc)
    {
        if (engine == null)
        {
        	engine = new minecraft_engine();
        	initialize_data();
        }
        if (!engine.safe_to_run())
        	return true;

        try
        {
        	if (engine.get_menu() != null)
        	{
        		return true;
        	} else
        		the_options.hide();
        	
	        int dimension = engine.get_current_dimension(last_dimension);
	        if (dimension != last_dimension)
	        {
	        	the_options.set(menu_options.options_indexes.CAVEMAP, dimension < 0);
	        	the_options.set(menu_options.options_indexes.HEIGHTMAP, !the_options.is_active(menu_options.options_indexes.CAVEMAP));
	            last_dimension = dimension;
	            save_main_settings();
	            if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
	                zoom = 1;
	        }
	
	        if (threading)
	        {
	
	            if (!calc_z.isAlive())
	            {
	                calc_z = new Thread(this);
	                calc_z.start();
	            }
	            
	            if (is_menu_showing() && !is_game_over() && !is_conflict_warning())
	                try
	                {
	                    calc_z.notify();
	                }
	                catch (Exception exception)
	                {
	                	exception.printStackTrace();
	                }
	        } else
	            if (the_options.is_active(menu_options.options_indexes.DISPLAY) && (last_x != coord_x() || last_z != coord_y() || timer > 300))
	                fill_map();
	
	        int screen_width = engine.get_screen_width() - 5,
	        	screen_height = engine.get_screen_height() - 100;
	
	        for (key_definition key : keys_list)
	        {
		        if (Keyboard.isKeyDown(key.value))
		        {
		        	switch (key.key)
		        	{
		        	case OPTIONS:
			        	the_options.show((screen_width + 5) / 2, (screen_height + 5) / 2);
		        		break;

		        	case WAYPOINTS:
		        		break;
		        	
		        	case ZOOM_IN:
				        set_zoom(true);
		        		break;
		        	
		        	case ZOOM_OUT:
			        	set_zoom(false);
		        		break;
		        	}
		        }
	        }

	        if (old_direction != engine.radius())
	        {
	            direction += old_direction - engine.radius();
	            old_direction = engine.radius();
	        }
	
	        if (direction >= 360.0f)
	        {
	        	while (direction >= 360.0f)
	        		direction -= 360.0f;
	        }
	
	        if (direction < 0.0f)
	        {
	            while (direction < 0.0f)
	                direction += 360.0f;
	        }
	
	        if (!info_message.isEmpty() && message_timer == 0)
	            message_timer = 500;
	        if (message_timer > 0)
	            message_timer--;
	        if (temp_counter > 0)
	        	temp_counter--;
	        if (message_timer == 0 && !info_message.isEmpty())
	            info_message = "";
	
            render_map(screen_width);

            if (message_timer > 0)
            	engine.draw_text(info_message, 20, 20, 0xffffff);

            if (the_options.is_active(menu_options.options_indexes.COORDINATES))
            	draw_coords(screen_width, screen_height);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
	
    public void run()
    {
        if (!engine.safe_to_run())
            return;

        while (true)
        {
            while (threading) 
            {
                for (active = true; engine.player_exists() && active; active = false)
                {
                    if (the_options.is_active(menu_options.options_indexes.DISPLAY) && (last_x != coord_x() || last_z != coord_y() || timer > 300) && engine.safe_to_run())
                        try
                        {
                            fill_map();
                            timer = 1;
                        }
                        catch (Exception exception)
                        {
                        	exception.printStackTrace();
                        }
                    timer++;
                }

                active = false;
                try
                {
                    Thread.sleep(10L);
                }
                catch (Exception exception)
                {
                	exception.printStackTrace();
                }
                try
                {
                    calc_z.wait(0L);
                }
                catch (Exception exception)
                {
                	exception.printStackTrace();
                }
            }
            try
            {
                Thread.sleep(1000L);
            }
            catch (Exception exception)
            {
            	exception.printStackTrace();
            }
            try
            {
                calc_z.wait(0L);
            }
            catch (Exception exception)
            {
            	exception.printStackTrace();
            }
        }
    }
    
    private void fill_map()
    {
        try
        {
            fd data = engine.get_world();
            int multi = (int)Math.pow(2, zoom);
            int start_x = coord_x();
            int start_z = coord_y();
            last_x = start_x;
            last_z = start_z;
            int color24 = 0;
            int surface_size = 32 * multi, half_surface = 16 * multi;
            start_x -= half_surface;
            start_z += half_surface;

            for (int image_y = 0; image_y < surface_size; image_y++)
            {
                for (int image_x = 0; image_x < surface_size; image_x++)
                {
                    color24 = 0;
                    boolean check = false;

                    if (calc_distance(half_surface, half_surface, image_y, image_x) < (half_surface - (int)Math.sqrt(multi)))
                        check = true;

                    int height = get_block_height(data, start_x + image_y, start_z - image_x);

                    if (check)
                    {
                        if (!the_options.is_active(menu_options.options_indexes.CAVEMAP))
                        {
                            if ((data.f(start_x + image_y, height + 1, start_z - image_x) == ln.s) || (data.f(start_x + image_y, height + 1, start_z - image_x) == ln.t))
                                color24 = 0xffffff;
                            else
                            {
                                BlockColor col = get_block_color(data.a(start_x + image_y, height, start_z - image_x), data.e(start_x + image_y, height, start_z - image_x));
                                color24 = col.color;
                            }

                        }
                        else
                            color24 = 0x808080;
                    }

                    if ((color24 != 0xff00ff) && (color24 != 0) && check)
                    {
                        if (the_options.is_active(menu_options.options_indexes.HEIGHTMAP))
                        {
                            int i2 = height;
                            //if offsetByZloc
                            i2 -= this.coord_z();
                            //else
                            //i2 -= 64;
                            double sc = Math.log10(Math.abs(i2) / 8.0D + 1.0D) / 1.3D;
                            int r = color24 / 0x10000;
                            int g = (color24 - r * 0x10000) / 0x100;
                            int b = (color24 - r * 0x10000 - g * 0x100);

                            if (i2 >= 0)
                            {
                                r = (int)(sc * (0xff - r)) + r;
                                g = (int)(sc * (0xff - g)) + g;
                                b = (int)(sc * (0xff - b)) + b;
                            }
                            else
                            {
                                i2 = Math.abs(i2);
                                r = r - (int)(sc * r);
                                g = g - (int)(sc * g);
                                b = b - (int)(sc * b);
                            }

                            color24 = r * 0x10000 + g * 0x100 + b;
                        }

                        int i3 = data.a(start_x + image_y, height + 1, start_z - image_x, false) * 17;
                        int min = 32;
                        if (i3 < min)
                        {
                            i3 = min;
                            if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
                                color24 = 0x222222;
                        }
                        if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
                            i3 *= 1.3f;
                        
                        if (i3 > 255)
                        	i3 = 255;
                        color24 = i3 * 0x1000000 + color24;
                    }

                    map[zoom].setRGB(image_x, image_y, color24);
                }
            }
            //	Fill the_entities list
            int position_x = engine.get_player_position_x(),
            	position_z = engine.get_player_position_y();
            the_entities.clear();
            List<Object> list = engine.get_entities_around(half_surface, 10, half_surface);
            for (int i = 0; i < list.size(); i++)
            {
                sn an_entity = (sn)list.get(i);
                if (!(an_entity instanceof sn))
                    continue;
                other_entity entity = new other_entity(an_entity);
                if (calc_distance(position_x, position_z, entity.position_x, entity.position_z) > (double)half_surface)
                    continue;
                entity.update_position(position_x, position_z, half_surface);
                if ((the_options.is_active(menu_options.options_indexes.MOBS) && entity.type != other_entity.entity_type.PLAYER && entity.type != other_entity.entity_type.ITEM) || (the_options.is_active(menu_options.options_indexes.PLAYERS) && entity.type == other_entity.entity_type.PLAYER))
                	the_entities.add(entity);
            }
        }
        catch (Throwable whatever)
        {
            whatever.printStackTrace();
        }
    }

    private void render_map(int screen_width)
    {
    	try
    	{
    		GL11.glDisable(GL11.GL_DEPTH_TEST);
	        GL11.glEnable(GL11.GL_BLEND);
	        GL11.glDepthMask(false);
	        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, 0);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	
	        if (the_options.is_active(menu_options.options_indexes.DISPLAY))
	        {
	            if (texture != 0)
	            	engine.renderize(texture);
	
            	GL11.glPushMatrix();
	            try
	            {
		            if (zoom == 3)
		            {
		                GL11.glPushMatrix();
			            try
			            {
			            	GL11.glScalef(0.5f, 0.5f, 1.0f);
			            	texture = engine.texturize(map[zoom]);
			            } catch (Exception e)
			            {
			                e.printStackTrace();
			            }
		            	GL11.glPopMatrix();
		            }
		            else
		            	texture = engine.texturize(map[zoom]);

		            GL11.glTranslatef(screen_width - 32.0F, 37.0F, 0.0F);
	                GL11.glRotatef(direction + 90.0F, 0.0F, 0.0F, 1.0F);
	                GL11.glTranslatef(-(screen_width - 32.0F), -37.0F, 0.0F);

	                if (zoom == 0)
	                    GL11.glTranslatef(-1.1f, -0.8f, 0.0f);
	                else
	                    GL11.glTranslatef(-0.5f, -0.5f, 0.0f);
		            
	            } catch (Exception e)
	            {
	                e.printStackTrace();
	            }
            	GL11.glPopMatrix();

            	engine.drawPre();
	            update_map(screen_width);
	            engine.drawPost();

	            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	
	            GL11.glColor3f(1.0F, 1.0F, 1.0F);
	            draw_round(screen_width);
	            draw_directions(screen_width);
	
	            the_waypoints.draw(coord_x(), coord_y(), the_options.is_active(menu_options.options_indexes.NETHERPOINTS), zoom, direction, screen_width);
	
            	GL11.glPushMatrix();
	            GL11.glDisable(GL11.GL_BLEND);
	            GL11.glDepthMask(true);
	            GL11.glDisable(GL11.GL_DEPTH_TEST);
	            
	            Iterator<other_entity> i$ = the_entities.iterator();
	            while (i$.hasNext())
	            {
	            	i$.next().draw(screen_width);
	            }            
            	GL11.glPopMatrix();
	
	            engine.drawPre();
	            update_map(screen_width);
	            engine.drawPost();
	        }
	        
    	} catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void draw_round(int screen_width)
    {
        try
        {
        	engine.load_image("/roundmap.png");
        	engine.drawPre();
            update_map(screen_width);
            engine.drawPost();
        }
        catch (Exception localException)
        {
            info_message = "Error: minimap overlay not found!";
        }
    }

    private void draw_directions(int screen_width)
    {
        GL11.glPushMatrix();
    	try
    	{
	        GL11.glScalef(0.5f, 0.5f, 1.0f);
	        GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction - 90.0D)))), (64.0D * Math.cos(Math.toRadians(-(this.direction - 90.0D)))), 0.0D);
	        engine.draw_text("N", screen_width * 2 - 66, 70, 0xffffff);
	        GL11.glPopMatrix();
	        GL11.glPushMatrix();
	        GL11.glScalef(0.5f, 0.5f, 1.0f);
	        GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-this.direction))), (64.0D * Math.cos(Math.toRadians(-this.direction))), 0.0D);
	        engine.draw_text("E", screen_width * 2 - 66, 70, 0xffffff);
	        GL11.glPopMatrix();
	        GL11.glPushMatrix();
	        GL11.glScalef(0.5f, 0.5f, 1.0f);
	        GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction + 90.0D)))), (64.0D * Math.cos(Math.toRadians(-(this.direction + 90.0D)))), 0.0D);
	        engine.draw_text("S", screen_width * 2 - 66, 70, 0xffffff);
	        GL11.glPopMatrix();
	        GL11.glPushMatrix();
	        GL11.glScalef(0.5f, 0.5f, 1.0f);
	        GL11.glTranslated((64.0D * Math.sin(Math.toRadians(-(this.direction + 180.0D)))), (64.0D * Math.cos(Math.toRadians(-(this.direction + 180.0D)))), 0.0D);
	        engine.draw_text("W", screen_width * 2 - 66, 70, 0xffffff);
    	} catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            GL11.glPopMatrix();
        }
    }
    
    private void draw_coords(int width, int height)
    {
        if (the_options.is_active(menu_options.options_indexes.DISPLAY))
        {
            GL11.glPushMatrix();
            try
            {
	            GL11.glScalef(0.5f, 0.5f, 1.0f);
	            String xy = coord_string(coord_x()) + ", " + coord_string(coord_y());
	            int m = engine.text_width(xy) / 2;
	            engine.draw_text(xy, width * 2 - 32 * 2 - m, 146, 0xffffff);
	            xy = Integer.toString(this.coord_z());
	            m = engine.text_width(xy) / 2;
	            engine.draw_text(xy, width * 2 - 32 * 2 - m, 156, 0xffffff);
            }  catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
            	GL11.glPopMatrix();
            }
        }
        else
        	engine.draw_text("(" + coord_string(coord_x()) + ", " + coord_z() + ", " + coord_string(coord_y()) + ") " + (int)direction + "'", 2, 10, 0xffffff);
    }

    private void set_zoom(boolean zoom_in)
    {
    	if (temp_counter > 0)
    		return;

    	if (zoom_in)
    		zoom--;
    	else
    		zoom++;
    	if (zoom < 0)
    		zoom = 0;
    	else if (zoom > 3)
    		zoom = 3;
        if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
        {
            info_message = "Cavemap zoom ";
            if (zoom > 1)
            {
                zoom = 1;
                info_message += "(2.0x)";
            }
            else
            {
                zoom = 0;
                info_message += "(4.0x)";
            }
        }
        switch (zoom)
        {
        case 3:
            info_message = "Zoom Level: (0.5x)";
            break;
            
        case 2:
            info_message = "Zoom Level: (1.0x)";
            break;
            
        case 1:
            info_message = "Zoom Level: (2.0x)";
            break;
        
        case 0:
            info_message = "Zoom Level: (4.0x)";
        }
        timer = 500;
        temp_counter = 20;
    }

    private void show_menu(int screen_width, int screen_height)
    {
/*        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int menu_item;
        int max_width = 0;
        int border = 2;
        boolean set = false;
        boolean click = false;
        int MouseX = engine.get_mouse_x(screen_width);
        int MouseY = engine.get_mouse_y(screen_height);

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0)
            if (!lfclick)
            {
                set = true;
                lfclick = true;
            }
            else
                click = true;
        else if (lfclick)
        	lfclick = false;

        String head, opt1, opt2, opt3 = "Remove";

        if (menu_select == menu_panel.OPTIONS)
        {
*           head = menu_options.get(options.TITLE);
            opt1 = "Exit Menu";
            opt2 = "Waypoints";
            for (options item : options.values())
                if (engine.text_width(menu_options.get(item)) > max_width)
                    max_width = engine.text_width(menu_options.get(item));
            menu_item = options.values().length;*/
        	the_options.show((screen_width + 5) / 2, (screen_height + 5) / 2);
/*        }
        else
        {
        	head = "Waypoints";
            opt1 = "Back";

            if (menu_select == menu_panel.WAYPOINTS_REMOVE)
                opt2 = "Cancel";
            else
                opt2 = "Add";

            max_width = the_waypoints.calc_width(80);

            menu_item = 10;
        }

        int title = engine.text_width(head);
        int centerX = (screen_width + 5) / 2;
        int centerY = (screen_height + 5) / 2;
        String choice;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        double leftX = centerX - title / 2.0D - border;
        double rightX = centerX + title / 2.0D + border;
        double topY = centerY - (menu_item - 1) * 5.0D - border - 20.0D;
        double botY = centerY - (menu_item - 1) * 5.0D + border - 10.0D;
        draw_box(leftX, rightX, topY, botY);

        leftX = centerX - max_width / 2.0D - 25 - border;
        rightX = centerX + max_width / 2.0D + 25 + border;
        topY = centerY - (menu_item - 1) * 5.0D - border;
        botY = centerY + (menu_item - 1) * 5.0D + border;
        draw_box(leftX, rightX, topY, botY);
        draw_options(rightX - border, topY + border, MouseX, MouseY, set, click);
        int footer = draw_footer(centerX, centerY, menu_item, opt1, opt2, opt3, border, MouseX, MouseY, set, click);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        engine.draw_text(head, centerX - title / 2, (centerY - (menu_item - 1) * 5) - 19, 0xffffff);

        if (menu_select == menu_panel.OPTIONS)
        {
            for (options item : options.values())
            {
            	engine.draw_text(menu_options.get(item), (int)leftX + border + 1, ((centerY - (menu_item - 1) * 5) + (item.ordinal() * 10)) - 9, 0xffffff);

                if (check_options(item))
                	choice = "On";
                else
                	choice = "Off";

                engine.draw_text(choice, (int)rightX - border - 15 - engine.text_width(choice) / 2, ((centerY - (menu_item - 1) * 5) + (item.ordinal() * 10)) - 8, 0xffffff);
            }
        }
        else
        {
            int max = first_entry + 9;

            if (max > the_waypoints.count())
            {
                max = the_waypoints.count();

                if (first_entry >= 0)
                {
                    if (max - 9 > 0)
                        first_entry = max - 9;
                    else
                        first_entry = 0;
                }
            }

            for (int n = first_entry; n < max; n++)
            {
            	Waypoint a_waypoint = the_waypoints.get(n);
                int yTop = ((centerY - (menu_item - 1) * 5) + ((n + 1 - first_entry) * 10));
                int leftTxt = (int)leftX + border + 1;
                engine.draw_text((n + 1) + ") " + a_waypoint.name, leftTxt, yTop - 9, 0xffffff);

                if (menu_select == menu_panel.WAYPOINTS_REMOVE)
                {
                	choice = "X";
                }
                else
                {
                    if (a_waypoint.enabled)
                    	choice = "On";
                    else
                    	choice = "Off";
                }

                engine.draw_text(choice, (int)rightX - border - 29 - engine.text_width(choice) / 2, yTop - 8, 0xffffff);

                if (MouseX > leftTxt && MouseX < (rightX - border - 77) && MouseY > yTop - 10 && MouseY < yTop - 1)
                {
                    String out = a_waypoint.x + ", " + a_waypoint.z;
                    int len = engine.text_width(out) / 2;
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.8f);
                    draw_box(MouseX - len - 1, MouseX + len + 1, MouseY - 11, MouseY - 1);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    engine.draw_text(out, MouseX - len, MouseY - 10, 0xffffff);
                }
            }
        }

        int footpos = ((screen_height + 5) / 2 + (menu_item - 1) * 5 + 11);

        if (menu_select == menu_panel.OPTIONS)
        {
        	engine.draw_text(opt1, centerX - 5 - border - footer - engine.text_width(opt1) / 2, footpos, 16777215);
        	engine.draw_text(opt2, centerX + border + 5 + footer - engine.text_width(opt2) / 2, footpos, 16777215);
        }
        else
        {
            if (menu_select != menu_panel.WAYPOINTS_REMOVE)
            	engine.draw_text(opt1, centerX - 5 - border * 2 - footer * 2 - engine.text_width(opt1) / 2, footpos, 16777215);

            engine.draw_text(opt2, centerX - engine.text_width(opt2) / 2, footpos, 16777215);

            if (menu_select != menu_panel.WAYPOINTS_REMOVE)
            	engine.draw_text(opt3, centerX + 5 + border * 2 + footer * 2 - engine.text_width(opt3) / 2, footpos, 16777215);
        }

        if (menu_select == menu_panel.WAYPOINT_NAME || menu_select == menu_panel.WAYPOINT_X || menu_select == menu_panel.WAYPOINT_Z)
        {
            String verify = "alf";
            try
            {
	            Waypoint new_waypoint = the_waypoints.create();
	
	            if ((menu_select == menu_panel.WAYPOINT_X || menu_select == menu_panel.WAYPOINT_Z) && input.isEmpty())
	                verify = "-0123456789n";
	            else if (menu_select == menu_panel.WAYPOINT_X || menu_select == menu_panel.WAYPOINT_Z)
	            	verify = "0123456789n";
	
	            if (Keyboard.getEventKeyState())
	            {
	                do
	                {
	                    if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && last_key != Keyboard.KEY_RETURN)
	                        if (input.isEmpty())
	                            next_menu = menu_panel.WAYPOINTS;
	                        else if (menu_select == menu_panel.WAYPOINT_NAME)
	                        {
	                            next_menu = menu_panel.WAYPOINT_X;
	                            new_waypoint.name = input;
	                            input = (netherpoints ? "n" : "") + Integer.toString(coord_x());
	                        }
	                        else if (menu_select == menu_panel.WAYPOINT_X)
	                        {
	                            next_menu = menu_panel.WAYPOINT_Z;
	
	                            try
	                            {
	                            	new_waypoint.x = to_int(input);
	                            }
	                            catch (Exception localException)
	                            {
	                                next_menu = menu_panel.WAYPOINTS;
	                            }
	
	                            input = (netherpoints ? "n" : "") + Integer.toString(coord_y());
	                        }
	                        else
	                        {
	                            next_menu = menu_panel.WAYPOINTS;
	
	                            try
	                            {
	                            	new_waypoint.z = to_int(input);
	                            }
	                            catch (Exception localException)
	                            {
	                                input = "";
	                            }
	
	                            if (!input.isEmpty())
	                            {
	                                the_waypoints.add(new_waypoint);
	                                the_waypoints.save();
	
	                                if (the_waypoints.count() > 9)
	                                	first_entry = the_waypoints.count() - 9;
	                            }
	                        }
	                    else if (Keyboard.getEventKey() == Keyboard.KEY_BACK && last_key != Keyboard.KEY_BACK)
	                        if (input.length() > 0)
	                            input = input.substring(0, input.length() - 1);
	
	                    if (verify.indexOf(Keyboard.getEventCharacter()) >= 0 && Keyboard.getEventKey() != last_key)
	                        if (engine.text_width(input + Keyboard.getEventCharacter()) < 148)
	                            input = input + Keyboard.getEventCharacter();
	
	                    last_key = Keyboard.getEventKey();
	                }
	                while (Keyboard.next());
	            }
	            else
	            	last_key = 0;
	
	            GL11.glDisable(GL11.GL_TEXTURE_2D);
	            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
	            leftX = centerX - 75 - border;
	            rightX = centerX + 75 + border;
	            topY = centerY - 10 - border;
	            botY = centerY + 10 + border;
	            draw_box(leftX, rightX, topY, botY);
	            leftX = leftX + border;
	            rightX = rightX - border;
	            topY = topY + 11;
	            botY = botY - border;
	            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
	            draw_box(leftX, rightX, topY, botY);
	            GL11.glEnable(GL11.GL_TEXTURE_2D);
	            String out = "Please enter a name:";
	
	            if (menu_select == menu_panel.WAYPOINT_X)
	                out = "Enter X coordinate:";
	            else if (menu_select == menu_panel.WAYPOINT_Z)
	            	out = "Enter Z coordinate:";
	
	            engine.draw_text(out, (int)leftX + border, (int)topY - 11 + border, 0xffffff);
	
	            if (blink > 60)
	            	blink = 0;
	
	            if (blink < 30)
	            	engine.draw_text(input + "|", (int)leftX + border, (int)topY + border, 0xffffff);
	            else
	            	engine.draw_text(input, (int)leftX + border, (int)topY + border, 0xffffff);
	
	            if (menu_select == menu_panel.WAYPOINT_X)
	                try
	                {
	                    if (to_int(input) == coord_x() * (netherpoints ? 8 : 1))
	                    	engine.draw_text("(Current)", (int)leftX + border + engine.text_width(input) + 5, (int)topY + border, 0xa0a0a0);
	                }
	                catch (Exception exception)
	                {
	                	exception.printStackTrace();
	                }
	            else if (menu_select == menu_panel.WAYPOINT_Z)
	                try
	                {
	                    if (to_int(input) == coord_y() * (netherpoints ? 8 : 1))
	                    	engine.draw_text("(Current)", (int)leftX + border + engine.text_width(input) + 5, (int)topY + border, 0xa0a0a0);
	                }
	                catch (Exception exception)
	                {
	                	exception.printStackTrace();
	                }

            }  catch (Exception e)
            {
                e.printStackTrace();
            }
            this.blink++;
        }
*/
    }
/*
    private int draw_footer(int centerX, int centerY, int m, String opt1, String opt2, String opt3, int border, int MouseX, int MouseY, boolean set, boolean click)
    {
        int footer = engine.text_width(opt1);

        if (engine.text_width(opt2) > footer) footer = engine.text_width(opt2);

        double leftX = centerX - footer - border * 2 - 5;
        double rightX = centerX - 5;
        double topY = centerY + (m - 1) * 5.0D - border + 10.0D;
        double botY = centerY + (m - 1) * 5.0D + border + 20.0D;

        if (menu_select != menu_panel.OPTIONS)
        {
            if (engine.text_width(opt3) > footer) footer = engine.text_width(opt3);

            leftX = centerX - border * 3 - footer * 1.5 - 5;
            rightX = centerX - footer / 2 - border - 5;
        }

        if (MouseX > leftX && MouseX < rightX && MouseY > topY && MouseY < botY && menu_select != menu_panel.WAYPOINTS_REMOVE)
            if (set || click)
            {
                if (set)
                {
                    if (menu_select == menu_panel.OPTIONS)
                    	engine.close_menu();
                    else
                        next_menu = menu_panel.OPTIONS;
                }

                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            }
            else
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
        else
            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

        if (menu_select != menu_panel.WAYPOINTS_REMOVE)
        	draw_box(leftX, rightX, topY, botY);

        if (menu_select == menu_panel.OPTIONS)
        {
            leftX = centerX + 5;
            rightX = centerX + footer + border * 2 + 5;
        }
        else
        {
            leftX = centerX - footer / 2 - border;
            rightX = centerX + footer / 2 + border;
        }

        if (MouseX > leftX && MouseX < rightX && MouseY > topY && MouseY < botY && (menu_select == menu_panel.OPTIONS || menu_select == menu_panel.WAYPOINTS || menu_select == menu_panel.WAYPOINTS_REMOVE))
            if (set || click)
            {
                if (set)
                {
                    if (menu_select == menu_panel.OPTIONS || menu_select == menu_panel.WAYPOINTS_REMOVE)
                        next_menu = menu_panel.WAYPOINTS;
                    else
                    {
                        next_menu = menu_panel.WAYPOINT_NAME;
                        input = "";
                    }
                }

                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            }
            else
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
        else
            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

        draw_box(leftX, rightX, topY, botY);

        if (menu_select != menu_panel.OPTIONS)
        {
            rightX = centerX + border * 3 + footer * 1.5 + 5;
            leftX = centerX + footer / 2 + border + 5;

            if (MouseX > leftX && MouseX < rightX && MouseY > topY && MouseY < botY && (menu_select == menu_panel.OPTIONS || menu_select == menu_panel.WAYPOINTS))
                if (set || click)
                {
                    if (set)
                    	next_menu = menu_panel.WAYPOINTS_REMOVE;

                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                }
                else
                    GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
            else
                GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);

            if (menu_select != menu_panel.WAYPOINTS_REMOVE)
            	draw_box(leftX, rightX, topY, botY);
        }

        return footer / 2;
    }

    private boolean check_options(options select)
    {
        switch (select)
        {
        case COORDINATES: return display_coords;
        case DISPLAY: return !hide;
        case LIGHTING: return mode_light;
        case HEIGHTMAP: return mode_height;
        case NETHERPOINTS: return netherpoints;
        case CAVEMAP: return mode_cave;
        default:
        	throw new IllegalArgumentException("bad option number " + select.ordinal());
        }
    }

    private void set_options(options select)
    {
        switch (select)
        {
        case COORDINATES:
        	display_coords = !display_coords;
        	break;
        case DISPLAY:
        	hide = !hide;
    		break;
        case LIGHTING:
    		mode_light = !mode_light;
            if (mode_cave)
                mode_height = !mode_light;
    		break;
        case HEIGHTMAP:
    		mode_height = !mode_height;
            if (mode_cave)
                mode_light = !mode_height;
    		break;
        case NETHERPOINTS:
        	netherpoints = !netherpoints;
    		break;
        case CAVEMAP:
            mode_cave = !mode_cave;
            if (mode_cave)
            {
                mode_light = true;
                mode_height = false;
                zoom = 1;
                info_message = "Cavemap zoom (2.0x)";
            }
        	break;
        default:
            throw new IllegalArgumentException("bad option number " + select.ordinal());
        }
        save_main_settings();
        timer = 50000;
    }
*/
    public boolean is_menu_showing()
    {
        return engine.get_menu() != null;
    }

    public boolean is_game_over()
    {
        return engine.get_menu() instanceof ch;
    }

    public boolean is_conflict_warning()
    {
        return engine.get_menu() instanceof qh;
    }

    private int coord_x()
    {
    	int x = engine.get_player_position_x();
        return x >= 0 ? x : x - 1;
    }

    private int coord_y()
    {
    	int y = engine.get_player_position_y();
        return y >= 0 ? y : y - 1;
    }

    private int coord_z()
    {
    	return engine.get_player_position_z();
    }

    private void update_map(int width)
    {
    	engine.ldrawthree(width - 64.0D, 64.0D + 5.0D, 1.0D, 0.0D, 1.0D);
    	engine.ldrawthree(width, 64.0D + 5.0D, 1.0D, 1.0D, 1.0D);
    	engine.ldrawthree(width, 5.0D, 1.0D, 1.0D, 0.0D);
    	engine.ldrawthree(width - 64.0D, 5.0D, 1.0D, 0.0D, 0.0D);
    }
/*
    private void draw_box(double left, double right, double top, double bottom)
    {
    	engine.drawPre();
    	engine.ldrawtwo(left, bottom, 0.0D);
    	engine.ldrawtwo(right, bottom, 0.0D);
    	engine.ldrawtwo(right, top, 0.0D);
    	engine.ldrawtwo(left, top, 0.0D);
    	engine.drawPost();
    }

    private void draw_options(double rightX, double topY, int MouseX, int MouseY, boolean set, boolean click)
    {
        if (menu_select == menu_panel.WAYPOINTS || menu_select == menu_panel.WAYPOINTS_REMOVE)
        {
            if (first_entry < 0)
            	first_entry = 0;

            if (!Mouse.isButtonDown(0) && scrollbar_click)
            	scrollbar_click = false;

            if (MouseX > (rightX - 10) && MouseX < (rightX - 2) && MouseY > (topY + 1) && MouseY < (topY + 10))
            {
                if (set || click)
                {
                    if (set && first_entry > 0)
                    	first_entry--;
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
                }
                else
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
            }
            else
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);

            engine.drawPre();
            engine.ldrawtwo(rightX - 10, topY + 10, 0.0D);
            engine.ldrawtwo(rightX - 2, topY + 10, 0.0D);
            engine.ldrawtwo(rightX - 6, topY + 1, 0.0D);
            engine.ldrawtwo(rightX - 6, topY + 1, 0.0D);
            engine.drawPost();

            if (the_waypoints.count() > 9)
            {
                scrollbar_size = 603 / the_waypoints.count();
            }
            else
            {
                scrollbar_offset = 0;
                scrollbar_size = 67;
            }

            if (MouseX > rightX - 10 && MouseX < rightX - 2 && MouseY > topY + 12 + scrollbar_offset && MouseY < topY + 12 + scrollbar_offset + scrollbar_size || scrollbar_click)
            {
                if (Mouse.isButtonDown(0) && !scrollbar_click)
                {
                    scrollbar_click = true;
                    scrollbar_start = MouseY;
                }
                else if (scrollbar_click && the_waypoints.count() > 9)
                {
                    int offset = MouseY - scrollbar_start;

                    if (scrollbar_offset + offset < 0)
                        scrollbar_offset = 0;
                    else if (scrollbar_offset + offset + scrollbar_size > 67)
                        scrollbar_offset = 67 - scrollbar_size;
                    else
                    {
                        scrollbar_offset = scrollbar_offset + offset;
                        scrollbar_start = MouseY;
                    }

                    first_entry = (int)((scrollbar_offset / (67 - scrollbar_size)) * (the_waypoints.count() - 9));
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
                }
                else
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
            }
            else
            {
                if (the_waypoints.count() > 9)
                    scrollbar_offset = (int)((double)first_entry / (double)(the_waypoints.count() - 9) * (67.0D - scrollbar_size));

                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
            }

            draw_box(rightX - 10, rightX - 2, topY + 12 + scrollbar_offset, topY + 12 + scrollbar_offset + scrollbar_size);

            if (MouseX > rightX - 10 && MouseX < rightX - 2 && MouseY > topY + 81 && MouseY < topY + 90)
            {
                if (set || click)
                {
                    if (set && first_entry < the_waypoints.count() - 9)
                    	first_entry++;

                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
                }
                else
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
            }
            else
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);

            engine.drawPre();
            engine.ldrawtwo(rightX - 6, topY + 90, 0.0D);
            engine.ldrawtwo(rightX - 6, topY + 90, 0.0D);
            engine.ldrawtwo(rightX - 2, topY + 81, 0.0D);
            engine.ldrawtwo(rightX - 10, topY + 81, 0.0D);
            engine.drawPost();
        }

        double leftX = rightX - 30;
        double botY = 0;
        topY += 1;
        int max = first_entry + 9;

        if (max > the_waypoints.count())
        {
            max = the_waypoints.count();

            if (first_entry > 0)
            {
                if (max - 9 > 0)
                    first_entry = max - 9;
                else
                    first_entry = 0;
            }
        }

        double leftCl = 0;
        double rightCl = 0;

        if (menu_select == menu_panel.WAYPOINTS || menu_select == menu_panel.WAYPOINTS_REMOVE)
        {
            leftX = leftX - 14;
            rightX = rightX - 14;
            rightCl = rightX - 32;
            leftCl = rightCl - 9;
        }
        else
        {
            first_entry = 0;
            max = menu_count;
        }

        for (int i = first_entry; i < max; i++)
        {
            if (i > first_entry) topY += 10;

            botY = topY + 9;

            if (MouseX > leftX && MouseX < rightX && MouseY > topY && MouseY < botY && (menu_select == menu_panel.OPTIONS || menu_select == menu_panel.WAYPOINTS || menu_select == menu_panel.WAYPOINTS_REMOVE))
                if (set || click)
                {
                    if (set)
                    {
                        if (menu_select == menu_panel.OPTIONS)
                            set_options(options.values()[i]);
                        else if (menu_select == menu_panel.WAYPOINTS)
                        {
                            the_waypoints.get(i).enabled = !the_waypoints.get(i).enabled;
                            the_waypoints.save();
                        }
                        else
                        {
                        	the_waypoints.delete(i);
                            next_menu = menu_panel.WAYPOINTS;
                        }
                    }

                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                }
                else
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
            else
            {
                if (menu_select == menu_panel.OPTIONS)
                {
                    if (check_options(options.values()[i]))
                        GL11.glColor4f(0.0f, 1.0f, 0.0f, 0.6f);
                    else
                        GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
                }
                else if (menu_select == menu_panel.WAYPOINTS_REMOVE)
                {
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.4f);
                }
                else
                {
                    if (the_waypoints.get(i).enabled)
                    {
                        GL11.glColor4f(0.0f, 1.0f, 0.0f, 0.6f);
                    }
                    else
                        GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
                }
            }

            draw_box(leftX, rightX, topY, botY);

            if ((menu_select == menu_panel.WAYPOINTS || menu_select == menu_panel.WAYPOINTS_REMOVE) && !(menu_select == menu_panel.WAYPOINTS_REMOVE && next_menu == menu_panel.WAYPOINTS))
            {
                if (MouseX > leftCl && MouseX < rightCl && MouseY > topY && MouseY < botY && menu_select == menu_panel.WAYPOINTS)
                    if (set)
                    {
                        the_waypoints.get(i).red = randomize.nextFloat();
                        the_waypoints.get(i).green = randomize.nextFloat();
                        the_waypoints.get(i).blue = randomize.nextFloat();
                        the_waypoints.save();
                    }

                GL11.glColor3f(the_waypoints.get(i).red, the_waypoints.get(i).green, the_waypoints.get(i).blue);
                draw_box(leftCl, rightCl, topY, botY);
            }
        }
    }
*/
    private final float calc_distance(int x1, int y1, int x2, int y2)
    {
        return (float)Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private final int get_block_height(fd world, int x, int z, int starty)
    {
        if (the_options.is_active(menu_options.options_indexes.CAVEMAP))
        {
            lm chunk = world.b(x, z);
            randomize.setSeed((x & 0xffff) | ((z & 0xffff) << 16));
            float distance = calc_distance(coord_x(), coord_y(), x, z);
            int y = coord_z();
            if (distance > 5)
                y -= (randomize.nextInt((int)(distance)) - ((int)distance / 2));
            x &= 0xf;
            z &= 0xf;
            
            
            if (y < 0)
                y = 0;
            else if (y > 127)
                y = 127;
            
            if (block_is_solid(chunk, x, y, z))
            {
                int itery = y;
                while (true)
                {
                    itery++;
                    if (itery > y + 10)
                        return y + 10;
                    if (!block_is_solid(chunk, x, itery, z))
                    {
                        return itery - 1;
                    }
                }
            }
            while (y > -1)
            {
                y--;
                if (block_is_solid(chunk, x, y, z))
                {
                    return y;
                }
            }
            return -1;
        }
        else
        {
            return world.d(x, z) - 1;
        }
    }

    private final int get_block_height(fd world, int x, int z)
    {
        return get_block_height(world, x, z, 127);
    }

    private final boolean block_is_solid(lm chunk, int x, int y, int z)
    {
        if (y > 127)
        	return false;
        if (y < 0)
        	return true;
        int id = chunk.a(x, y, z);
        int meta = chunk.b(x, y, z);
        return get_block_color(id, meta).alpha > 0;
    }

    private final int block_color_ID(int blockid, int meta)
    {
        return (blockid) | (meta << 8);
    }

    private final BlockColor get_block_color(int blockid, int meta)
    {
        try
        {
            BlockColor col = block_colors[block_color_ID(blockid, meta)];
            if (col != null)
            	return col;
            col = block_colors[block_color_ID(blockid, 0)];
            if (col != null)
            	return col;
            col = block_colors[0];
            if (col != null)
            	return col;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            System.err.println("BlockID: " + blockid + " - Meta: " + meta);
            throw e;
        }
        System.err.println("Unable to find a block color for blockid: " + blockid + " blockmeta: " + meta);
        return new BlockColor(0xff00ff, 0xff, TintType.NONE);
    }

    private String coord_string(int coord_value)
    {
        if (coord_value < 0)
            return "-" + Math.abs(coord_value + 1);
        else
            return "+" + coord_value;
    }

    public int to_int(String str)
    {
        if (str.startsWith("n"))
        {
            return Integer.parseInt(str.substring(1)) * 8;
        }
        else
        {
            return Integer.parseInt(str);
        }
    }

    public String Version()
    {
        return "1.7.3.0";
    }


    //	My data
    private minecraft_engine engine;
    
    private BufferedImage map[];

    public String[] main_menu;
    public menu_options the_options;
    public int menu_count;
    public boolean scrollbar_click;
    public int scrollbar_start, scrollbar_offset, scrollbar_size, first_entry;
    //	Cursor blink interval
    public int blink;
    //	Last key down on previous render
    public int last_key;
    public String input; 

    private BlockColor block_colors[];
    //	Was mouse down last render?
    public boolean lfclick = false;
    //	Holds error exceptions thrown
    public String info_message;

    public boolean active;
    private int zoom;
    private int last_x, last_z;
    public int texture; 
    private float direction, old_direction;
    //	Waypoint names and data
    public waypoints the_waypoints;
    public List<other_entity> the_entities;
    private int timer, message_timer, temp_counter;
    public Thread calc_z;
    public Random randomize;
    private boolean threading;
    //	last dimension of rendering
    private int last_dimension;
    public static mod_MyMiniMap instance;
}
