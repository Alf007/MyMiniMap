import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**	Interface to Minecraft client
*/
public class minecraft_engine
{
	private Minecraft game;
    //	Font rendering class
	private sj lang;
    //	Render texture
	private ji render_engine;
	//	Polygon creation class
	private nw lDraw;

	public minecraft_engine()
	{
		game = ModLoader.getMinecraftInstance();
    	lang = game.q;
        render_engine = game.p;
		lDraw = nw.a;
	}
	
    public File get_app_dir(String app)
    {
        return Minecraft.a(app);
    }

    public boolean safe_to_run()
    {
        return game != null && game.i != null;
    }
    
    public boolean player_exists()
    {
        return game.h != null;
    }

    public void close_menu()
    {
        game.r = null;
    }

    public void show_generic_gui()
    {
        this.game.a(new da());
    }

    public Object get_menu()
    {
        return ((Object) (game.r));
    }

    public int get_mouse_x(int screen_width)
    {
        return Mouse.getX() * (screen_width + 5) / game.d;
    }

    public int get_mouse_y(int screen_height)
    {
        return (screen_height + 5) - Mouse.getY() * (screen_height + 5) / this.game.e - 1;
    }

    public void to_chat(String s)
    {
        game.v.a(s);
    }
    
    public int text_width(String text)
    {
        return lang.a(text);
    }

    public void draw_text(String text, int x, int y, int color)
    {
        lang.a(text, x, y, color);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void draw_image(String texture_name, int x, int y, int width, int height, int offset_x, int offset_y, float scale, float rotation)
    {
    	GL11.glBindTexture(GL11.GL_TEXTURE_2D, get_texture(texture_name));
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glRotatef(rotation, 0.0F, 0.0F, 1.0F);
        GL11.glTranslatef(-x, -y, 0.0F);
    	game.v.b(x - width / 2, y - height / 2, offset_x, offset_y, width, height);	
        GL11.glPopMatrix();
    }
    
    public gs get_player()
    {
		return (gs)game.h;
    }

    public String get_map_name()
    {
        return game.f.x.j();
    }

    public String get_server_name()
    {
        return game.z.C;
    }

    public class location
    {
    	public int x, y, z;
    	
    	public location(int x, int y, int z)
    	{
    		this.x = x;
    		this.y = y;	
    		this.z = z;
    	}
    }
    
    public int get_player_position_x()
    {
    	gs player = get_player();
    	return (int)player.aM;
    }
    
    public int get_player_position_y()
    {
    	gs player = get_player();
    	return (int)player.aO - 1;
    }

    public int get_player_position_z()
    {
    	gs player = get_player();
    	return (int)player.aN;
    }
    
    public List<Object> get_entities_around(int width, int height, int length)
    {
    	return (List<Object>)game.f.b(((sn) (get_player())), game.h.aW.b(width, height, length));
    }
    
    public int get_screen_width()
    {
	    qq screen = new qq(game.z, game.d, game.e);
	    return screen.a();
    }
    
    public int get_screen_height()
    {
	    qq screen = new qq(game.z, game.d, game.e);
	    return screen.b();
    }

    public fd get_world()
    {
        return game.f;
    }

    public int get_current_dimension(int last_dimension)
    {
        if (!player_exists())
            return last_dimension;
        return game.h.m;
    }

    public void renderize(int g)
    {
        render_engine.a(g);
    }

    public int texturize(BufferedImage paramImg)
    {
        return render_engine.a(paramImg);
    }

    public void load_image(String image_name)
    {
    	render_engine.b(get_texture(image_name));
    }

    public int get_texture(String image_name)
    {
    	return render_engine.b(image_name);
    }

    public void drawPre()
    {
        lDraw.b();
    }

    public void drawPost()
    {
        lDraw.a();
    }

    public void ldrawone(int a, int b, double c, double d, double e)
    {
        lDraw.a(a, b, c, d, e);
    }

    public void ldrawtwo(double a, double b, double c)
    {
        lDraw.a(a, b, c);
    }

    public void ldrawthree(double a, double b, double c, double d, double e)
    {
        lDraw.a(a, b, c, d, e);
    }

    public float radius()
    {
    	gs player = get_player();
        return player.aS;
    }
}
