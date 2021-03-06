package de.cas_ual_ty.ydm.duelmanager.playfield;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import de.cas_ual_ty.ydm.duelmanager.DuelManager;

public class PlayField
{
    public final DuelManager duelManager;
    public final PlayFieldType playFieldType;
    public final List<Zone> zones;
    
    public byte player1Offset;
    public byte player2Offset;
    public byte extraOffset;
    
    public Zone player1Deck;
    public Zone player1ExtraDeck;
    public Zone player2Deck;
    public Zone player2ExtraDeck;
    
    public PlayField(DuelManager duelManager, PlayFieldType type)
    {
        if(type.player1Deck == null || type.player1ExtraDeck == null || type.player2Deck == null || type.player2ExtraDeck == null)
        {
            throw new IllegalArgumentException();
        }
        
        this.duelManager = duelManager;
        this.playFieldType = type;
        this.zones = new ArrayList<>(type.zoneEntries.size());
        
        byte index = 0;
        Zone z;
        for(PlayFieldType.ZoneEntry e : type.zoneEntries)
        {
            this.zones.add(z = new Zone(this, e.type, index++, e.owner, e.x, e.y, e.width, e.height));
            
            if(e == type.player1Deck)
            {
                this.player1Deck = z;
            }
            else if(e == type.player1ExtraDeck)
            {
                this.player1ExtraDeck = z;
            }
            else if(e == type.player2Deck)
            {
                this.player2Deck = z;
            }
            else if(e == type.player2ExtraDeck)
            {
                this.player2ExtraDeck = z;
            }
        }
        
        this.zones.sort((z1, z2) ->
        {
            int x1;
            int x2;
            
            if(z1.owner == ZoneOwner.PLAYER1)
            {
                x1 = -1;
            }
            else if(z1.owner == ZoneOwner.PLAYER2)
            {
                x1 = 0;
            }
            else
            {
                x1 = 1;
            }
            
            if(z2.owner == ZoneOwner.PLAYER1)
            {
                x2 = -1;
            }
            else if(z2.owner == ZoneOwner.PLAYER2)
            {
                x2 = 0;
            }
            else
            {
                x2 = 1;
            }
            
            return x1 - x2;
        });
        
        this.player1Offset = 0;
        this.player2Offset = 0;
        this.extraOffset = 0;
        
        for(Zone zone : this.zones)
        {
            if(zone.getOwner() == ZoneOwner.PLAYER2)
            {
                break;
            }
            
            ++this.player2Offset;
        }
        
        for(Zone zone : this.zones)
        {
            if(zone.getOwner() == ZoneOwner.NONE)
            {
                break;
            }
            
            ++this.extraOffset;
        }
    }
    
    public List<Zone> getZones()
    {
        return this.zones;
    }
    
    public List<ZoneInteraction> getActionsFor(ZoneOwner player, Zone interactor, @Nullable DuelCard interactorCard, Zone interactee)
    {
        return this.playFieldType.getActionsFor(player, interactor, interactorCard, interactee);
    }
    
    public Zone getReplacementZoneForCard(Zone zone, DuelCard card)
    {
        /*
        if(!zone.getType().getIsStrict())
        {
            // zone isnt strict (eg. player1's monster zones allow player2's cards in them)
            return zone;
        }
        else
        {
            // zone is strict, and zone owner is card owner
            if(zone.getOwner() == card.getOwner())
            {
                return zone;
            }
            else
            {
                // zone is strict, and zone owner is not card owner
                // swap for equivalent zone, shift by offset
                return this.zones[ZoneOwner.convertIndex(zone.getIndex())];
            }
        }
        */ return null;
    }
    
    @Nullable
    public Zone getZoneByTypeAndPlayer(ZoneType type, ZoneOwner owner)
    {
        for(Zone zone : this.zones)
        {
            if(zone.type == type && zone.getOwner() == owner && !zone.getIsOwnerTemporary())
            {
                return zone;
            }
        }
        
        return null;
    }
    
    public DuelManager getDuelManager()
    {
        return this.duelManager;
    }
    
    public Zone getZone(byte zoneId)
    {
        return this.zones.get(zoneId);
    }
}
