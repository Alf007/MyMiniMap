/** Generic class to read settings from a file
 *  as key:value entries
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;

import net.minecraft.client.Minecraft;

public class file_settings
{
	private minecraft_engine engine;
	private File the_file;
	private HashMap<String, String> settings;
	private PrintWriter for_output;
	private boolean mode_write;

    public static File get_app_dir(String app)
    {
        return Minecraft.a(app);
    }

    public static File get_config_dir(String app)
    {
        return new File(get_app_dir(app), "config");
    }

	public file_settings(String filename)
	{
		engine = new minecraft_engine();
		mode_write = false;
		try
		{
	    	settings = new HashMap<String, String>();
			the_file = new File(get_config_dir("minecraft"), filename);
            if (the_file.exists())
            {
                BufferedReader file_read = new BufferedReader(new FileReader(the_file));
                String line;
                while ((line = file_read.readLine()) != null)
                {
                    if (line.startsWith("#"))
                    	continue;
                    String[] key_value = line.split(":", 2);
                    settings.put(key_value[0], key_value[1]);
                }
                file_read.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	public void finalize()
	{
		if (mode_write)
		{
			for_output.close();
		}
	}
	
	public boolean has(String key)
	{
		return settings.containsKey(key);
	}
	public String get(String key)
	{
		return settings.get(key);
	}
	public Set<String> keys()
	{
		return settings.keySet();
	}

	public void prepare_for_output()
	{
		if (!mode_write)
		{
			try {
				for_output = new PrintWriter(new FileWriter(the_file));
			} catch (IOException e) {
				e.printStackTrace();
			}
			mode_write = true;
		} else
		{	//	Restart
			for_output.close();
			try {
				for_output = new PrintWriter(new FileWriter(the_file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void print(String text)
	{
		if (!mode_write)
			prepare_for_output();
        try
        {
        	for_output.print(text);
        }
        catch (Exception local)
        {
        	engine.to_chat("§EError Saving Settings");
        }
	}
	
	public void println(String text)
	{
		if (!mode_write)
			prepare_for_output();
        try
        {
        	for_output.println(text);
        }
        catch (Exception local)
        {
        	engine.to_chat("§EError Saving Settings");
        }
	}
	public void println()
	{
		if (!mode_write)
			prepare_for_output();
        try
        {
        	for_output.println();
        }
        catch (Exception local)
        {
        	engine.to_chat("§EError Saving Settings");
        }
	}
}
