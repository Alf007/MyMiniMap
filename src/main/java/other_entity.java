

public class other_entity
{
	private minecraft_engine engine;

	public enum entity_type { FRIEND, ENEMY, PLAYER, NEUTRAL, ALLY, ITEM };

	public other_entity(sn entity)
	{
		engine = new minecraft_engine();
		position_x = (int)entity.aM;
		position_z = (int)entity.aO;
        if ((entity instanceof gi) && ((gi)entity).D())
            type = entity_type.NEUTRAL;
        else if ((entity instanceof bg) || (entity instanceof ar) || (entity instanceof uw))
            type = entity_type.FRIEND;
        else if (entity instanceof gz)
            type = entity_type.ENEMY;
        else if ((entity instanceof ls) && !(entity instanceof gs))
            type = entity_type.ALLY;
            
	}
	public void update_position(int center_x, int center_z, int half_surface)
	{
		position_x = position_x - center_x + half_surface;
        position_z = position_z - center_z + half_surface - 1;
	}
	
	public void draw(int screen_width)
	{
        int x = screen_width - 32 + position_x,
        	y = 37 + position_z,
        	w = 8, h = 8,
        	mx = 0, my = 0;
        float scale = 0.5f,
        	rotation = 0F;
        switch (type)
        {
        case FRIEND:
            mx = 0;
            break;
        case ENEMY:
            mx = 8;
            break;
        case PLAYER:
            mx = 16;
            break;
        case NEUTRAL:
            mx = 24;
            break;
        case ALLY:
            mx = 32;
            break;
        case ITEM:
            mx = 40;
            scale = 0.4F;
            break;
        }
        engine.draw_image("/entities.png", x, y, w, h, mx, my, scale, rotation);
	}
	
	public int position_x, position_z;
	public entity_type type;
}
