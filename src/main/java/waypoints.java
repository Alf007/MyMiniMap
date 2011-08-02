import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.opengl.GL11;

public class waypoints
{
	private minecraft_engine engine;

	public waypoints()
	{
		engine = new minecraft_engine();
		current_world = "";
		the_waypoints = new ArrayList<Waypoint>();
		load();
	}
	
    public boolean load()
    {
        try
        {
            String j;
        	String mapName = engine.get_map_name();
	        if (mapName.equals("MpServer"))
	        {
	            String[] i = engine.get_server_name().toLowerCase().split(":");
	            j = i[0];
	        }
	        else
	            j = mapName;
	
	        if (!current_world.equals(j))
	        {
	        	current_world = j;
	            the_waypoints.clear();
	
	            file_settings waypoints_settings = new file_settings(new String(current_world + ".points"));
	
	            try
	            {
	                if (waypoints_settings.keys().isEmpty())
	                	engine.to_chat("§EError: No waypoint exist for this world/server.");
	                else
	                {
	                    Set<String> key_ids = waypoints_settings.keys();
	                    Iterator<String> key_id =  key_ids.iterator();
	                    while (key_id.hasNext())
	                    {
	                    	String key = key_id.next(),
	                    		value = waypoints_settings.get(key),
	                    		values[] = value.split(":");
	                        if (values.length == 3)
	                        	the_waypoints.add(new Waypoint(key, Integer.parseInt(values[1]), Integer.parseInt(values[2]), Boolean.parseBoolean(values[3])));
	                        else
	                        	the_waypoints.add(new Waypoint(key, Integer.parseInt(values[1]), Integer.parseInt(values[2]), Boolean.parseBoolean(values[3]), Float.parseFloat(values[4]), Float.parseFloat(values[5]), Float.parseFloat(values[6])));
	                    }
	                    engine.to_chat("§EWaypoints loaded for " + current_world);
	                }
	            }
	            catch (Exception local)
	            {
	            	engine.to_chat("§EError Loading Waypoints");
	            }
	            return true;
	        }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public void save()
    {
        try
        {
        	file_settings waypoints_settings = new file_settings(new String(current_world + ".points"));
        	waypoints_settings.prepare_for_output();

        	if (!the_waypoints.isEmpty())
	            for (Waypoint pt : the_waypoints)
	            {
	                if (!pt.name.startsWith("^"))
	                	waypoints_settings.println(pt.name + ":" + pt.x + ":" + pt.z + ":" + Boolean.toString(pt.enabled) + ":" + pt.red + ":" + pt.green + ":" + pt.blue);
	            }

        }
        catch (Exception local)
        {
        	engine.to_chat("§EError Saving Waypoints");
        }
    }

    public void delete(int i)
    {
    	try
    	{
    		the_waypoints.remove(i);
    	} catch (Exception e)
    	{
    		e.printStackTrace();
        }
        save();
    }

	public boolean isEmpty()
	{
		return the_waypoints.isEmpty();
	}

	public int count()
	{
		return the_waypoints.size();
	}
	
	public Waypoint get(int index)
	{
		return the_waypoints.get(index);
	}
	
	public void add(Waypoint new_point)
	{
		try
		{
			the_waypoints.add(new_point);
		} catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	public Waypoint create()
	{
		return new Waypoint("", 0, 0, true);
	}
	
	public void draw(int x, int y, boolean nether, int zoom, float direction, int screen_width)
	{
    	if (!isEmpty())
    	{
            for (Waypoint pt : the_waypoints)
            {
                if (pt.enabled)
                {
                    int wayX = x - (pt.x / (nether ? 8 : 1));
                    int wayY = y - (pt.z / (nether ? 8 : 1));
                    float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
                    double hypot = Math.sqrt((wayX * wayX) + (wayY * wayY)) / (Math.pow(2, zoom) / 2);

                    if (hypot >= 31.0D)
                    {
                        try
                        {
                            GL11.glPushMatrix();
                            GL11.glColor3f(pt.red, pt.green, pt.blue);
                            engine.load_image("/marker.png");
                            GL11.glTranslatef(screen_width - 32.0F, 37.0F, 0.0F);
                            GL11.glRotatef(-locate + direction + 180.0F, 0.0F, 0.0F, 1.0F);
                            GL11.glTranslatef(-(screen_width - 32.0F), -37.0F, 0.0F);
                            GL11.glTranslated(0.0D, -34.0D, 0.0D);
                        }
                        catch (Exception localException)
                        {
                        	engine.to_chat("Error: marker overlay not found!");
                        }
                        finally
                        {
                            GL11.glPopMatrix();
                        }
                    }
                }
            }

            for (Waypoint pt : the_waypoints)
            {
                if (pt.enabled)
                {
                    int wayX = x - (pt.x / (nether ? 8 : 1));
                    int wayY = y - (pt.z / (nether ? 8 : 1));
                    float locate = (float)Math.toDegrees(Math.atan2(wayX, wayY));
                    double hypot = Math.sqrt((wayX * wayX) + (wayY * wayY)) / (Math.pow(2, zoom) / 2);

                    if (hypot < 31.0D)
                    {
                        try
                        {
                            GL11.glPushMatrix();
                            GL11.glColor3f(pt.red, pt.green, pt.blue);
                            engine.load_image("/waypoint.png");
                            GL11.glTranslatef(screen_width - 32.0F, 37.0F, 0.0F);
                            GL11.glRotatef(-locate + direction + 180.0F, 0.0F, 0.0F, 1.0F);
                            GL11.glTranslated(0.0D, -hypot, 0.0D);
                            GL11.glRotatef(-(-locate + direction + 180.0F), 0.0F, 0.0F, 1.0F);
                            GL11.glTranslated(0.0D, hypot, 0.0D);
                            GL11.glTranslatef(-(screen_width - 32.0F), -37.0F, 0.0F);
                            GL11.glTranslated(0.0D, -hypot, 0.0D);
                        }
                        catch (Exception localException)
                        {
                        	engine.to_chat("Error: waypoint overlay not found!");
                        }
                        finally
                        {
                            GL11.glPopMatrix();
                        }
                    }
                }
            }
    	}
	}
	
	public int calc_width(int max_width)
	{
        for (int i = 0; i < the_waypoints.size(); i++)
            if (engine.text_width((i + 1) + ") " + the_waypoints.get(i).name) > max_width)
                max_width = engine.text_width((i + 1) + ") " + the_waypoints.get(i).name) + 32;
        return max_width;
	}
	
    public String current_world;
    public ArrayList<Waypoint> the_waypoints;
}
