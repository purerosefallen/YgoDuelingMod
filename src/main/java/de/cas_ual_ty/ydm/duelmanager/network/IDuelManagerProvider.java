package de.cas_ual_ty.ydm.duelmanager.network;

import java.util.List;

import de.cas_ual_ty.ydm.deckbox.DeckHolder;
import de.cas_ual_ty.ydm.duelmanager.DeckSource;
import de.cas_ual_ty.ydm.duelmanager.DuelManager;
import de.cas_ual_ty.ydm.duelmanager.DuelState;
import de.cas_ual_ty.ydm.duelmanager.PlayerRole;
import de.cas_ual_ty.ydm.duelmanager.action.Action;

public interface IDuelManagerProvider
{
    public DuelManager getDuelManager();
    
    public default DuelMessageHeader getMessageHeader()
    {
        return this.getDuelManager().headerFactory.get();
    }
    
    public default void updateDuelState(DuelState duelState)
    {
        this.getDuelManager().setDuelStateAndUpdate(duelState);
    }
    
    public default void handleAction(Action action)
    {
        action.init(this.getDuelManager().getPlayField());
        action.doAction();
    }
    
    public default void handleAllActions(List<Action> actions)
    {
        // just do all actions without animation
        
        for(Action action : actions)
        {
            action.init(this.getDuelManager().getPlayField());
            action.doAction();
        }
    }
    
    public default void receiveDeckSources(List<DeckSource> deckSources)
    {
    }
    
    public default void receiveDeck(int index, DeckHolder deck)
    {
    }
    
    public default void deckAccepted(PlayerRole role)
    {
    }
}
