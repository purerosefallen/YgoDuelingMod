package de.cas_ual_ty.ydm.duelmanager.action;

import java.util.List;
import java.util.Random;

import de.cas_ual_ty.ydm.duelmanager.playfield.DuelCard;
import de.cas_ual_ty.ydm.duelmanager.playfield.Zone;
import net.minecraft.network.PacketBuffer;

public class ShuffleAction extends SingleZoneAction
{
    protected List<DuelCard> before;
    protected List<DuelCard> after;
    protected long randomSeed;
    
    public ShuffleAction(ActionType actionType, byte sourceZoneId, long randomSeed)
    {
        super(actionType, sourceZoneId);
    }
    
    public ShuffleAction(ActionType actionType, byte sourceZoneId)
    {
        this(actionType, sourceZoneId, System.nanoTime());
    }
    
    public ShuffleAction(ActionType actionType, Zone sourceZone)
    {
        this(actionType, sourceZone.index);
    }
    
    public ShuffleAction(ActionType actionType, PacketBuffer buf)
    {
        this(actionType, buf.readByte(), buf.readLong());
    }
    
    @Override
    public void writeToBuf(PacketBuffer buf)
    {
        super.writeToBuf(buf);
        buf.writeLong(this.randomSeed);
    }
    
    @Override
    public void doAction()
    {
        this.before = this.sourceZone.getCardsList();
        this.sourceZone.shuffle(new Random(this.randomSeed)); //TODO instead use random from a playfield, and synch the random seed before all actions are synched
        this.after = this.sourceZone.getCardsList();
    }
    
    @Override
    public void undoAction()
    {
        this.sourceZone.setCardsList(this.before);
    }
    
    @Override
    public void redoAction()
    {
        this.sourceZone.setCardsList(this.after);
    }
}
