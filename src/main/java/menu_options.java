import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

public class menu_options implements ItemListener
{
	public enum options_indexes { DISPLAY, COORDINATES, LIGHTING, HEIGHTMAP, CAVEMAP, NETHERPOINTS, PLAYERS, MOBS };
	public class option_definition
	{
		options_indexes option;
		boolean active;
		String label;

		public option_definition(options_indexes option, String label)
		{
			this.option = option;
			this.label = label;
			this.active = true;
		}
	}
	static public ArrayList<option_definition> options_list = new ArrayList<option_definition>();
	public JPopupMenu popup;
	private String filename;
	private minecraft_engine engine; 

	public menu_options(minecraft_engine engine, String filename)
	{
		this.engine = engine;
		options_list.add(new option_definition(options_indexes.DISPLAY, "Show map")); 
		options_list.add(new option_definition(options_indexes.COORDINATES, "Show Coordinates"));
		options_list.add(new option_definition(options_indexes.LIGHTING, "Dynamic Lighting"));
		options_list.add(new option_definition(options_indexes.HEIGHTMAP, "Terrain Depth"));
		options_list.add(new option_definition(options_indexes.CAVEMAP, "Cavemap"));
		options_list.add(new option_definition(options_indexes.NETHERPOINTS, "Netherpoints"));
		options_list.add(new option_definition(options_indexes.PLAYERS, "Display players"));
		options_list.add(new option_definition(options_indexes.MOBS, "Display Mobs"));
		this.filename = filename;
		load();
	}

	public void load()
	{
		try
		{
	        file_settings main_settings = new file_settings(filename);
			Iterator<option_definition> menu_item = options_list.iterator();
			while (menu_item.hasNext())
			{
				option_definition current = menu_item.next(); 
		        if (main_settings.has(current.label))
		            current.active = Boolean.parseBoolean(main_settings.get(current.label));
			}
		} catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}

	public void save()
	{
		try
		{
	        file_settings main_settings = new file_settings(filename);
			Iterator<option_definition> menu_item = options_list.iterator();
			while (menu_item.hasNext())
			{
				option_definition current = menu_item.next(); 
		    	main_settings.prepare_for_output();
		    	main_settings.println(current.label + ":" + Boolean.toString(current.active));
			}
		} catch (Exception e)
        {
            e.printStackTrace();
        }
	}

	public void show(int center_x, int center_y)
	{
		try
		{
			popup = new JPopupMenu();
			Iterator<option_definition> menu_item = options_list.iterator();
			while (menu_item.hasNext())
			{
				option_definition option = menu_item.next(); 
				JCheckBoxMenuItem item = new JCheckBoxMenuItem(option.label, option.active);
				item.addItemListener(this);
				popup.add(item);
			}
			popup.show(engine.get_component(), center_x, center_y);
		} catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	public void itemStateChanged(ItemEvent e)
	{
		JCheckBoxMenuItem menu_item = (JCheckBoxMenuItem) e.getItemSelectable();
		get_option(menu_item.getText()).active = menu_item.isSelected();
	};

	public option_definition get_option(options_indexes option)
	{
		Iterator<option_definition> menu_item = options_list.iterator();
		while (menu_item.hasNext())
		{
			option_definition current = menu_item.next(); 
			if (current.option == option)
				return current;
		}
		return null;
	}

	public option_definition get_option(String label)
	{
		Iterator<option_definition> menu_item = options_list.iterator();
		while (menu_item.hasNext())
		{
			option_definition current = menu_item.next(); 
			if (current.label == label)
				return current;
		}
		return null;
	}
	
	public boolean is_active(options_indexes option)
	{
		return get_option(option).active;
	}
	
	public void set(options_indexes option, boolean active)
	{
		get_option(option).active = active;
	}
}
