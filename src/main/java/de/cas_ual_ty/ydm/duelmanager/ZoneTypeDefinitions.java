package de.cas_ual_ty.ydm.duelmanager;

import javax.annotation.Nullable;

public class ZoneTypeDefinitions
{
    public static final ZoneTypeDefinitions HAND = new ZoneTypeDefinitions((byte)0, 13, 227);
    public static final ZoneTypeDefinitions DECK = new ZoneTypeDefinitions((byte)1, 106, 193).defaultCardPosition(CardPosition.FACE_DOWN);
    public static final ZoneTypeDefinitions SPELL_TRAP = new ZoneTypeDefinitions((byte)2, 68, 193).withChildren(5, -2);
    public static final ZoneTypeDefinitions EXTRA_DECK = new ZoneTypeDefinitions((byte)7, -90, 193);
    public static final ZoneTypeDefinitions GRAVEYARD = new ZoneTypeDefinitions((byte)8, 106, 159).strict();
    public static final ZoneTypeDefinitions MONSTER = new ZoneTypeDefinitions((byte)9, 68, 159).allowSideways().withChildren(5, -2);
    public static final ZoneTypeDefinitions FIELD_SPELL = new ZoneTypeDefinitions((byte)14, -90, 159);
    public static final ZoneTypeDefinitions BANISHED = new ZoneTypeDefinitions((byte)15, 106, 125).strict();
    public static final ZoneTypeDefinitions EXTRA = new ZoneTypeDefinitions((byte)16, -90, 227);
    
    // TODO coordinates
    // index starts back at 0 because they have no owner. player offset will be added automatically
    public static final ZoneTypeDefinitions EXTRA_MONSTER_1 = new ZoneTypeDefinitions((byte)0, 0, 0).noOwner();
    public static final ZoneTypeDefinitions EXTRA_MONSTER_2 = new ZoneTypeDefinitions((byte)1, 0, 0).noOwner();
    
    public final byte index;
    
    // position of bottom zones ("your" zones, not "enemy" zones)
    public final int x;
    public final int y;
    
    private int width;
    private int height;
    
    // for description of other fields, see builder methods below
    
    private boolean straightOnly;
    private boolean isStrict;
    
    private int childrenAmt;
    private int childOffX;
    
    @Nullable
    public CardPosition defaultCardPosition;
    
    public boolean noOwner;
    
    public ZoneTypeDefinitions(byte index, int x, int y)
    {
        this.index = index;
        this.x = x;
        this.y = y;
        
        this.width = -1;
        this.height = -1;
        
        this.straightOnly = true;
        this.isStrict = false;
        
        this.childrenAmt = 1;
        this.childOffX = 0;
        
        this.defaultCardPosition = null;
        
        this.noOwner = false;
    }
    
    public ZoneTypeDefinitions fullSize()
    {
        this.width = 32;
        this.height = 32;
        return this;
    }
    
    public ZoneTypeDefinitions slimSize()
    {
        this.width = 24;
        this.height = 32;
        return this;
    }
    
    public ZoneTypeDefinitions size(int width, int height)
    {
        this.width = width;
        this.height = height;
        return this;
    }
    
    // allow SET and DEF position
    public ZoneTypeDefinitions allowSideways()
    {
        this.straightOnly = false;
        return this;
    }
    
    // dont allow enemy cards in zone (GY, banished)
    public ZoneTypeDefinitions strict()
    {
        this.isStrict = true;
        return this;
    }
    
    // amt: amount of children (eg. Monster Zone has 5)
    // childOffX: space between them (width already accounted for)
    public ZoneTypeDefinitions withChildren(int childrenAmt, int childOffX)
    {
        this.childrenAmt = childrenAmt;
        this.childOffX = childOffX;
        
        if(this.childOffX < 0)
        {
            this.childOffX -= this.width;
        }
        
        if(this.childOffX > 0)
        {
            this.childOffX += this.width;
        }
        
        return this;
    }
    
    // change the default card position when
    public ZoneTypeDefinitions defaultCardPosition(CardPosition defaultCardPosition)
    {
        this.defaultCardPosition = defaultCardPosition;
        return this;
    }
    
    // for the link zones; does not create this zone once for each side
    public ZoneTypeDefinitions noOwner()
    {
        this.noOwner = true;
        return this;
    }
    
    public byte getIndex()
    {
        return this.index;
    }
    
    public int getX()
    {
        return this.x;
    }
    
    public int getY()
    {
        return this.y;
    }
    
    public int getWidth()
    {
        return this.width;
    }
    
    public int getHeight()
    {
        return this.height;
    }
    
    public boolean getStraightOnly()
    {
        return this.straightOnly;
    }
    
    public boolean getIsStrict()
    {
        return this.isStrict;
    }
    
    public int getChildrenAmount()
    {
        return this.childrenAmt;
    }
    
    public byte getIndexForChild(byte child)
    {
        return (byte)(this.index + child);
    }
    
    public int getXOffsetForChild(byte child)
    {
        return child * this.childOffX;
    }
    
    @Nullable
    public CardPosition getDefaultCardPosition()
    {
        return this.defaultCardPosition;
    }
    
    public boolean getNoOwner()
    {
        return this.noOwner;
    }
}
