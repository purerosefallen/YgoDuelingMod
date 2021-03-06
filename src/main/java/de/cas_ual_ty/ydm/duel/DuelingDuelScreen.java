package de.cas_ual_ty.ydm.duel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.cas_ual_ty.ydm.YDM;
import de.cas_ual_ty.ydm.clientutil.ClientProxy;
import de.cas_ual_ty.ydm.clientutil.YdmBlitUtil;
import de.cas_ual_ty.ydm.duelmanager.DuelManager;
import de.cas_ual_ty.ydm.duelmanager.PlayerRole;
import de.cas_ual_ty.ydm.duelmanager.action.ActionIcon;
import de.cas_ual_ty.ydm.duelmanager.network.DuelMessages;
import de.cas_ual_ty.ydm.duelmanager.playfield.DuelCard;
import de.cas_ual_ty.ydm.duelmanager.playfield.Zone;
import de.cas_ual_ty.ydm.duelmanager.playfield.ZoneInteraction;
import de.cas_ual_ty.ydm.duelmanager.playfield.ZoneOwner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class DuelingDuelScreen extends DuelContainerScreen<DuelContainer> implements IDuelScreenContext
{
    public static final ResourceLocation DUEL_FOREGROUND_GUI_TEXTURE = new ResourceLocation(YDM.MOD_ID, "textures/gui/duel_foreground.png");
    public static final ResourceLocation DUEL_BACKGROUND_GUI_TEXTURE = new ResourceLocation(YDM.MOD_ID, "textures/gui/duel_background.png");
    
    public static final int CARDS_WIDTH = 24;
    public static final int CARDS_HEIGHT = 32;
    
    protected ViewCardStackWidget viewCardStackWidget;
    protected Button scrollUpButton;
    protected Button scrollDownButton;
    
    protected ZoneWidget clickedZoneWidget;
    protected DuelCard clickedCard;
    
    protected List<ZoneWidget> zoneWidgets;
    protected List<InteractionWidget> interactionWidgets;
    
    protected ZoneOwner view;
    
    public DuelingDuelScreen(DuelContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.interactionWidgets = new ArrayList<>(); // Need to temporarily initialize with placeholder this to make sure no clear() call gets NPEd
        this.xSize = 234;
        this.ySize = 250;
        this.viewCardStackWidget = null;
        this.clickedZoneWidget = null;
        this.clickedCard = null;
        this.view = this.getZoneOwner();
        if(this.view == ZoneOwner.NONE)
        {
            this.view = ZoneOwner.PLAYER1;
        }
    }
    
    @Override
    public void init(Minecraft minecraft, int width, int height)
    {
        super.init(minecraft, width, height);
        
        this.zoneWidgets = new ArrayList<>(this.getDuelManager().getPlayField().getZones().size());
        this.interactionWidgets.clear();
        
        ZoneWidget widget;
        
        for(Zone zone : this.getDuelManager().getPlayField().getZones())
        {
            this.addButton(widget = new ZoneWidget(zone, this, zone.width, zone.height, StringTextComponent.EMPTY, this::zoneClicked, this::zoneTooltip));
            
            if(this.getPlayerRole() == ZoneOwner.PLAYER2.player)
            {
                widget.setPositionRelativeFlipped(zone.x, zone.y, width, height);
            }
            else
            {
                widget.setPositionRelative(zone.x, zone.y, width, height);
            }
            
            this.zoneWidgets.add(widget);
        }
        
        int maxWidth = (this.width - this.xSize) / 2;
        int maxHeight = this.height - 40;
        int x = this.width - maxWidth / 2;
        int y = this.height / 2;
        int cardsSize = 32;
        
        int w = 3 * cardsSize;
        int h = 3 * cardsSize;
        
        ViewCardStackWidget previousViewStack = this.viewCardStackWidget;
        this.addButton(this.viewCardStackWidget = new ViewCardStackWidget(this, x - w / 2, y - h / 2, w, h, StringTextComponent.EMPTY, this::viewCardStackClicked, this::viewCardStackTooltip)
            .setRowsAndColumns(cardsSize, 3, 3));
        //TODO
        
        int verticalButtonsOff = 4;
        this.addButton(this.scrollUpButton = new Button(x - w / 2, y - h / 2 - 20 - verticalButtonsOff, w, 20, new StringTextComponent("Up"), this::scrollButtonClicked));
        this.addButton(this.scrollDownButton = new Button(x - w / 2, y + h / 2 + verticalButtonsOff, w, 20, new StringTextComponent("Down"), this::scrollButtonClicked));
        
        // in case we init again, buttons is cleared, thus all interaction widgets are removed
        // just act like we click on the last widget again
        if(this.clickedZoneWidget != null)
        {
            for(ZoneWidget match : this.zoneWidgets)
            {
                if(match.zone == this.clickedZoneWidget.zone)
                {
                    this.clickedZoneWidget = match;
                    break;
                }
            }
            
            this.clickedZoneWidget.hoverCard = this.clickedCard;
            this.zoneClicked(this.clickedZoneWidget);
        }
        
        if(previousViewStack != null && previousViewStack.cards != null)
        {
            this.viewCards(previousViewStack.cards, previousViewStack.forceFaceUp);
        }
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int x, int y)
    {
        DuelingDuelScreen.renderDisabledRect(ms, 0, 0, this.width, this.height);
        
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(DuelingDuelScreen.DUEL_BACKGROUND_GUI_TEXTURE);
        this.blit(ms, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.minecraft.getTextureManager().bindTexture(DuelingDuelScreen.DUEL_FOREGROUND_GUI_TEXTURE);
        this.blit(ms, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int x, int y)
    {
        
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(button == 1)
        {
            this.removeClickedZone();
            this.removeInteractionWidgets();
            
            for(ZoneWidget w : this.zoneWidgets)
            {
                w.active = true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    public void flip()
    {
        if(this.view == ZoneOwner.PLAYER1)
        {
            this.view = ZoneOwner.PLAYER2;
        }
        else
        {
            this.view = ZoneOwner.PLAYER1;
        }
        
        for(ZoneWidget w : this.zoneWidgets)
        {
            w.flip(this.width, this.height);
        }
    }
    
    protected void zoneClicked(ZoneWidget widget)
    {
        if(!widget.active)
        {
            return;
        }
        
        ZoneOwner owner = this.getZoneOwner();
        
        if(owner == ZoneOwner.NONE)
        {
            return;
        }
        
        this.clickedZoneWidget = widget;
        this.clickedCard = widget.hoverCard;
        
        this.removeInteractionWidgets();
        
        this.interactionWidgets = new ArrayList<>();
        
        for(ZoneWidget w : this.zoneWidgets)
        {
            w.addInteractionWidgets(owner, this.clickedZoneWidget.zone, this.clickedCard, this.getDuelManager(), this.interactionWidgets, this::interactionClicked, this::interactionTooltip);
            w.active = false;
        }
        
        this.buttons.addAll(this.interactionWidgets);
        this.children.addAll(this.interactionWidgets);
        
        if(!widget.zone.getType().getIsSecret())
        {
            this.viewZone(widget, owner == widget.zone.getOwner() && widget.zone.type.getShowFaceDownCardsToOwner());
        }
    }
    
    protected void viewZone(ZoneWidget widget, boolean forceFaceUp)
    {
        this.viewCards(widget.zone.getCardsList(), forceFaceUp);
    }
    
    protected void viewCards(List<DuelCard> cards, boolean forceFaceUp)
    {
        this.viewCardStackWidget.activate(cards, forceFaceUp);
    }
    
    public void viewZone(Zone zone)
    {
        for(ZoneWidget w : this.zoneWidgets)
        {
            if(w.zone == zone)
            {
                this.removeClickedZone();
                this.removeInteractionWidgets();
                this.activateZoneWidgets();
                
                w.hoverCard = null;
                
                this.zoneClicked(w);
                this.viewZone(w, true);
                return;
            }
        }
    }
    
    public void viewCards(Zone zone, List<DuelCard> cards)
    {
        for(ZoneWidget w : this.zoneWidgets)
        {
            if(w.zone == zone)
            {
                this.removeClickedZone();
                this.removeInteractionWidgets();
                this.activateZoneWidgets();
                
                w.hoverCard = null;
                
                this.zoneClicked(w);
                this.viewCardStackWidget.activate(cards, true);
                return;
            }
        }
    }
    
    protected void interactionClicked(InteractionWidget widget)
    {
        YDM.channel.send(PacketDistributor.SERVER.noArg(), new DuelMessages.RequestDuelAction(this.getDuelManager().headerFactory.get(), widget.interaction.action));
        
        this.removeClickedZone();
        this.removeInteractionWidgets();
        this.activateZoneWidgets();
    }
    
    protected void viewCardStackClicked(ViewCardStackWidget widget)
    {
        ZoneWidget w = this.clickedZoneWidget;
        boolean forceFaceUp = widget.forceFaceUp;
        
        if(w != null)
        {
            w.active = true;
            w.hoverCard = widget.hoverCard;
            this.zoneClicked(w);
            
            if(forceFaceUp)
            {
                widget.forceFaceUp();
            }
        }
    }
    
    protected void scrollButtonClicked(Button button)
    {
        if(this.viewCardStackWidget.active)
        {
            if(button == this.scrollUpButton)
            {
                this.viewCardStackWidget.decreaseCurrentRow();
            }
            else if(button == this.scrollDownButton)
            {
                this.viewCardStackWidget.increaseCurrentRow();
            }
        }
    }
    
    protected void activateZoneWidgets()
    {
        for(ZoneWidget w : this.zoneWidgets)
        {
            w.active = true;
        }
    }
    
    protected void deactivateZoneWidgets()
    {
        for(ZoneWidget w : this.zoneWidgets)
        {
            w.active = false;
        }
    }
    
    protected void removeInteractionWidgets()
    {
        this.buttons.removeIf((w) -> w instanceof InteractionWidget);
        this.children.removeIf((w) -> w instanceof InteractionWidget);
    }
    
    protected void removeClickedZone()
    {
        this.clickedZoneWidget = null;
        this.clickedCard = null;
        this.viewCardStackWidget.deactivate();
    }
    
    protected void zoneTooltip(Widget w0, MatrixStack ms, int mouseX, int mouseY)
    {
        ZoneWidget w = (ZoneWidget)w0;
        this.renderTooltip(ms, new StringTextComponent(w.zone.getType().getRegistryName().getPath() + " " + w.zone.getOwner().name()), mouseX, mouseY);
    }
    
    protected void interactionTooltip(Widget w0, MatrixStack ms, int mouseX, int mouseY)
    {
        InteractionWidget w = (InteractionWidget)w0;
        this.renderTooltip(ms, new StringTextComponent(w.interaction.icon.getRegistryName().getPath()), mouseX, mouseY);
    }
    
    protected void viewCardStackTooltip(Widget w0, MatrixStack ms, int mouseX, int mouseY)
    {
    }
    
    @Override
    public Zone getClickedZone()
    {
        return this.clickedZoneWidget != null ? this.clickedZoneWidget.zone : null;
    }
    
    @Override
    public DuelCard getClickedDuelCard()
    {
        return this.clickedCard;
    }
    
    @Override
    public ZoneOwner getView()
    {
        return this.view;
    }
    
    @Override
    public ZoneOwner getZoneOwner()
    {
        PlayerRole role = this.getPlayerRole();
        
        if(ZoneOwner.PLAYER1.player == role)
        {
            return ZoneOwner.PLAYER1;
        }
        else if(ZoneOwner.PLAYER2.player == role)
        {
            return ZoneOwner.PLAYER2;
        }
        else
        {
            return ZoneOwner.NONE;
        }
    }
    
    @Override
    public void renderCardInfo(MatrixStack ms, DuelCard card)
    {
        ClientProxy.renderCardInfo(ms, card.getCardHolder(), (this.width - this.xSize) / 2);
    }
    
    public static void renderHoverRect(MatrixStack ms, int x, int y, int w, int h)
    {
        // from ContainerScreen#render
        
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        ClientProxy.drawRect(ms, x, y, w, h, 1F, 1F, 1F, 0.5F);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }
    
    public static void renderDisabledRect(MatrixStack ms, int x, int y, int w, int h)
    {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        ClientProxy.drawRect(ms, x, y, w, h, 0F, 0F, 0F, 0.5F);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }
    
    public static void renderSelectedRect(MatrixStack ms, int x, int y, int w, int h)
    {
        RenderSystem.disableDepthTest();
        ClientProxy.drawLineRect(ms, x - 1, y - 1, w + 2, h + 2, 2, 0, 0, 1F, 1F);
        RenderSystem.enableDepthTest();
    }
    
    public static void renderEnemySelectedRect(MatrixStack ms, int x, int y, int w, int h)
    {
        RenderSystem.disableDepthTest();
        ClientProxy.drawLineRect(ms, x - 1, y - 1, w + 2, h + 2, 2, 1F, 0, 0, 1F);
        RenderSystem.enableDepthTest();
    }
    
    public static void renderCardWith(MatrixStack ms, int x, int y, int width, int height, DuelCard card, YdmBlitUtil.FullBlitMethod blitMethod, boolean forceFaceUp)
    {
        Minecraft mc = ClientProxy.getMinecraft();
        
        // bind the texture depending on faceup or facedown
        if(card.getCardPosition().isFaceUp || forceFaceUp)
        {
            ClientProxy.bindMainResourceLocation(card.getCardHolder());
        }
        else
        {
            mc.getTextureManager().bindTexture(ClientProxy.getMainCardBack());
        }
        
        blitMethod.fullBlit(ms, x, y, width, height);
        
        if(card.getIsToken())
        {
            mc.getTextureManager().bindTexture(ClientProxy.getMainCardBack());
            blitMethod.fullBlit(ms, x, y, width, height);
        }
    }
    
    public static void renderCard(MatrixStack ms, int x, int y, int width, int height, DuelCard card, boolean forceFaceUp)
    {
        DuelingDuelScreen.renderCardWith(ms, x, y, width, height, card,
            card.getCardPosition().isStraight
                ? YdmBlitUtil::fullBlit
                : YdmBlitUtil::fullBlit90Degree, forceFaceUp);
    }
    
    public static void renderCardReversed(MatrixStack ms, int x, int y, int width, int height, DuelCard card, boolean forceFaceUp)
    {
        DuelingDuelScreen.renderCardWith(ms, x, y, width, height, card,
            card.getCardPosition().isStraight
                ? YdmBlitUtil::fullBlit180Degree
                : YdmBlitUtil::fullBlit270Degree, forceFaceUp);
    }
    
    public static void renderCardCentered(MatrixStack ms, int x, int y, int width, int height, DuelCard card, boolean forceFaceUp)
    {
        // if width and height are more of a rectangle, this centers the texture horizontally
        x -= (height - width) / 2;
        width = height;
        
        DuelingDuelScreen.renderCard(ms, x, y, width, height, card, forceFaceUp);
    }
    
    public static void renderCardReversedCentered(MatrixStack ms, int x, int y, int width, int height, DuelCard card, boolean forceFaceUp)
    {
        // if width and height are more of a rectangle, this centers the texture horizontally
        x -= (height - width) / 2;
        width = height;
        
        DuelingDuelScreen.renderCardReversed(ms, x, y, width, height, card, forceFaceUp);
    }
    
    // TODO make more specialized zone widgets
    // maybe with client registration via zone type
    public static class ZoneWidget extends Button
    {
        public final Zone zone;
        public final IDuelScreenContext context;
        public boolean isFlipped;
        public DuelCard hoverCard;
        
        public ZoneWidget(Zone zone, IDuelScreenContext context, int width, int height, ITextComponent title, Consumer<ZoneWidget> onPress, ITooltip onTooltip)
        {
            super(0, 0, width, height, title, (w) -> onPress.accept((ZoneWidget)w), onTooltip);
            this.zone = zone;
            this.context = context;
            this.shift();
            this.hoverCard = null;
        }
        
        protected void shift()
        {
            this.x -= this.width / 2;
            this.y -= this.height / 2;
        }
        
        protected void unshift()
        {
            this.x += this.width / 2;
            this.y += this.height / 2;
        }
        
        public ZoneWidget flip(int guiWidth, int guiHeight)
        {
            guiWidth /= 2;
            guiHeight /= 2;
            
            this.unshift();
            
            this.x -= guiWidth;
            this.y -= guiHeight;
            
            this.x = -this.x;
            this.y = -this.y;
            
            this.x += guiWidth;
            this.y += guiHeight;
            
            this.shift();
            
            this.isFlipped = !this.isFlipped;
            
            return this;
        }
        
        public ZoneWidget setPositionRelative(int x, int y, int guiWidth, int guiHeight)
        {
            this.x = x + guiWidth / 2;
            this.y = y + guiHeight / 2;
            
            this.shift();
            
            this.isFlipped = false;
            
            return this;
        }
        
        public ZoneWidget setPositionRelativeFlipped(int x, int y, int guiWidth, int guiHeight)
        {
            this.x = guiWidth / 2 - x;
            this.y = guiHeight / 2 - y;
            
            this.shift();
            
            this.isFlipped = true;
            
            return this;
        }
        
        @Override
        public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partialTicks)
        {
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer fontrenderer = minecraft.fontRenderer;
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            
            if(this.context.getClickedZone() == this.zone && this.context.getClickedDuelCard() == null)
            {
                DuelingDuelScreen.renderSelectedRect(ms, this.x, this.y, this.width, this.height);
            }
            
            this.hoverCard = this.renderCards(ms, mouseX, mouseY);
            //            ClientProxy.drawLineRect(ms, this.x, this.y, this.width, this.height, 1, 1, 0, 0, 1);
            
            int j = this.getFGColor();
            AbstractGui.drawCenteredString(ms, fontrenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
            
            if(this.active)
            {
                if(this.isHovered())
                {
                    if(this.zone.getCardsAmount() == 0)
                    {
                        DuelingDuelScreen.renderHoverRect(ms, this.x, this.y, this.width, this.height);
                    }
                    
                    this.renderToolTip(ms, mouseX, mouseY);
                }
            }
            else
            {
                DuelingDuelScreen.renderDisabledRect(ms, this.x, this.y, this.width, this.height);
            }
        }
        
        @Nullable
        public DuelCard renderCards(MatrixStack ms, int mouseX, int mouseY)
        {
            int cardsWidth = DuelingDuelScreen.CARDS_WIDTH * this.height / DuelingDuelScreen.CARDS_HEIGHT;
            int cardsHeight = this.height;
            int cardsTextureSize = cardsHeight;
            
            DuelCard hoveredCard = null;
            int hoverX = this.x;
            int hoverY = this.y;
            int hoverWidth = this.width;
            int hoverHeight = this.height;
            
            boolean isOwner = this.zone.getOwner() == this.context.getZoneOwner();
            boolean isOpponentView = this.zone.getOwner() != this.context.getView();
            
            if(this.zone.type.getRenderCardsSpread())
            {
                DuelCard c = null;
                hoverWidth = cardsWidth;
                
                int totalW = this.zone.getCardsAmount() * cardsWidth;
                
                if(totalW <= this.width)
                {
                    int x = this.x + (this.width - totalW) / 2;
                    int renderX = x - (cardsTextureSize - cardsWidth) / 2; // Cards are 24x32, but the textures are still 32x32, so we must account for that
                    int y = this.y;
                    
                    for(short i = 0; i < this.zone.getCardsAmount(); ++i)
                    {
                        if(!isOpponentView)
                        {
                            c = this.zone.getCard(i);
                        }
                        else
                        {
                            c = this.zone.getCard((short)(this.zone.getCardsAmount() - i - 1));
                        }
                        
                        if(this.drawCard(ms, c, renderX, y, cardsTextureSize, cardsTextureSize, mouseX, mouseY, x, y, cardsWidth, cardsHeight))
                        {
                            hoveredCard = c;
                            hoverX = x;
                            hoverY = y;
                        }
                        
                        x += cardsWidth;
                        renderX += cardsWidth;
                    }
                }
                else
                {
                    int x = this.x;
                    int y = this.y;
                    
                    int x1;
                    int renderX1;
                    
                    float margin = cardsWidth - (this.zone.getCardsAmount() * cardsWidth - this.width) / (float)(this.zone.getCardsAmount() - 1);
                    
                    boolean renderLeftToRight = !this.zone.type.getRenderCardsReversed() && isOpponentView; // wenn true
                    boolean renderFrontToBack = this.zone.type.getRenderCardsReversed() && isOpponentView; // flip
                    
                    if(!renderLeftToRight)
                    {
                        margin *= -1F;
                        x += this.width - cardsWidth;
                    }
                    
                    int renderX = x - (cardsTextureSize - cardsWidth) / 2; // Cards are 24x32, but the textures are still 32x32, so we must account for that
                    
                    for(short i = 0; i < this.zone.getCardsAmount(); ++i)
                    {
                        if(renderFrontToBack)
                        {
                            c = this.zone.getCard(i);
                        }
                        else
                        {
                            c = this.zone.getCard((short)(this.zone.getCardsAmount() - i - 1));
                        }
                        
                        x1 = x + (int)(i * margin);
                        renderX1 = renderX + (int)(i * margin);
                        
                        // if this is the top rendered card
                        // and the card is sideways
                        // adjust the hover rect
                        // and also render it centered again
                        if(c == this.zone.getTopCardSafely() && !c.getCardPosition().isStraight)
                        {
                            int renX = this.x + (this.width - cardsTextureSize) / 2;
                            int renY = this.y + (this.height - cardsTextureSize) / 2;
                            
                            int offset = (cardsHeight - cardsWidth);
                            int hovX = renX;
                            int hovY = renY + offset / 2;
                            int hovW = cardsHeight;
                            int hovH = cardsWidth;
                            
                            if(this.drawCard(ms, c, renX, renY, cardsTextureSize, cardsTextureSize, mouseX, mouseY, hovX, hovY, hovW, hovH))
                            {
                                hoveredCard = c;
                                hoverX = hovX;
                                hoverY = hovY;
                                hoverWidth = hovW;
                                hoverHeight = hovH;
                            }
                        }
                        else if(this.drawCard(ms, c, renderX1, y, cardsTextureSize, cardsTextureSize, mouseX, mouseY, x1, y, cardsWidth, cardsHeight))
                        {
                            hoveredCard = c;
                            hoverX = x1;
                            hoverY = y;
                        }
                    }
                }
            }
            else
            {
                DuelCard c = this.zone.getTopCardSafely();
                
                if(c != null && this.drawCard(ms, c, this.x, this.y, this.width, this.height, mouseX, mouseY, this.x, this.y, this.width, this.height))
                {
                    hoveredCard = c;
                }
                
                // #drawCard only draws the top card here
                // so only if the top card is selected, this zone is marked
                // so we gotta mark the zone in case this zone is selected
                if(this.context.getClickedDuelCard() != c && this.context.getClickedZone() == this.zone)
                {
                    DuelingDuelScreen.renderSelectedRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
                }
            }
            
            if(hoveredCard != null)
            {
                if(hoveredCard.getCardPosition().isFaceUp || (isOwner && !this.zone.getType().getIsSecret()))
                {
                    this.context.renderCardInfo(ms, hoveredCard);
                }
                
                if(this.active)
                {
                    DuelingDuelScreen.renderHoverRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
                }
            }
            
            if(!this.active)
            {
                return null;
            }
            else
            {
                return hoveredCard;
            }
        }
        
        protected boolean drawCard(MatrixStack ms, DuelCard duelCard, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY, int hoverX, int hoverY, int hoverWidth, int hoverHeight)
        {
            boolean isOwner = this.zone.getOwner() == this.context.getZoneOwner();
            boolean faceUp = this.zone.getType().getShowFaceDownCardsToOwner() && isOwner;
            boolean isOpponentView = this.zone.getOwner() != this.context.getView();
            
            if(duelCard == this.context.getClickedDuelCard())
            {
                DuelingDuelScreen.renderSelectedRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
            }
            
            if(!isOpponentView)
            {
                DuelingDuelScreen.renderCardCentered(ms, renderX, renderY, renderWidth, renderHeight, duelCard, faceUp);
            }
            else
            {
                DuelingDuelScreen.renderCardReversedCentered(ms, renderX, renderY, renderWidth, renderHeight, duelCard, faceUp);
            }
            
            if(this.isHovered() && mouseX >= hoverX && mouseX < hoverX + hoverWidth && mouseY >= hoverY && mouseY < hoverY + hoverHeight)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        
        public void addInteractionWidgets(ZoneOwner player, Zone interactor, DuelCard interactorCard, DuelManager m, List<InteractionWidget> list, Consumer<InteractionWidget> onPress, ITooltip onTooltip)
        {
            List<ZoneInteraction> interactions = m.getActionsFor(player, interactor, interactorCard, this.zone);
            
            if(interactions.size() == 0)
            {
                return;
            }
            
            if(interactions.size() == 1)
            {
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
            }
            else if(interactions.size() == 2)
            {
                if(this.width <= this.height)
                {
                    // Split them horizontally (1 action on top, 1 on bottom)
                    list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 2, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                }
                else
                {
                    // Split them vertically (1 left, 1 right)
                    list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 2, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 2, this.y, this.width / 2, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                }
            }
            else if(interactions.size() == 3)
            {
                if(this.width == this.height)
                {
                    // 1 on top half, 1 bottom left, 1 bottom right
                    list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(2), this.context, this.x + this.width / 2, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                }
                else if(this.width < this.height)
                {
                    // Horizontally split
                    list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 3, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(2), this.context, this.x, this.y + this.height * 2 / 3, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
                }
                else //if(this.width > this.height)
                {
                    // Vertically split
                    list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 3, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                    list.add(new InteractionWidget(interactions.get(2), this.context, this.x + this.width * 2 / 3, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                }
            }
            else if(interactions.size() == 4 && this.width == this.height)
            {
                // 1 on top left, 1 top right, 1 bottom left, 1 bottom right
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 2, this.y, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(2), this.context, this.x, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(3), this.context, this.x + this.width / 2, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            }
            else
            {
                if(this.width < this.height)
                {
                    // Horizontally split
                    for(int i = 0; i < interactions.size(); ++i)
                    {
                        list.add(new InteractionWidget(interactions.get(i), this.context, this.x, this.y + this.height * i / interactions.size(), this.width, this.height / interactions.size(), StringTextComponent.EMPTY, onPress, onTooltip));
                    }
                }
                else //if(this.width > this.height)
                {
                    // Vertically split
                    for(int i = 0; i < interactions.size(); ++i)
                    {
                        list.add(new InteractionWidget(interactions.get(i), this.context, this.x + this.width * i / interactions.size(), this.y, this.width / interactions.size(), this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                    }
                }
            }
        }
    }
    
    public static class InteractionWidget extends Button
    {
        public final ZoneInteraction interaction;
        public final IDuelScreenContext context;
        
        public InteractionWidget(ZoneInteraction interaction, IDuelScreenContext context, int x, int y, int width, int height, ITextComponent title, Consumer<InteractionWidget> onPress, ITooltip onTooltip)
        {
            super(x, y, width, height, title, (w) -> onPress.accept((InteractionWidget)w), onTooltip);
            this.interaction = interaction;
            this.context = context;
        }
        
        @Override
        public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partialTicks)
        {
            ActionIcon icon = this.interaction.icon;
            
            int iconWidth = icon.iconWidth;
            int iconHeight = icon.iconHeight;
            
            if(iconHeight >= this.height)
            {
                iconWidth = this.height * iconWidth / iconHeight;
                iconHeight = this.height;
            }
            
            if(iconWidth >= this.width)
            {
                iconHeight = this.width * iconHeight / iconWidth;
                iconWidth = this.width;
            }
            
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer fontrenderer = minecraft.fontRenderer;
            
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            
            ClientProxy.getMinecraft().getTextureManager().bindTexture(icon.sourceFile);
            YdmBlitUtil.blit(ms, this.x + (this.width - iconWidth) / 2, this.y + (this.height - iconHeight) / 2, iconWidth, iconHeight, icon.iconX, icon.iconY, icon.iconWidth, icon.iconHeight, icon.fileSize, icon.fileSize);
            
            int j = this.getFGColor();
            AbstractGui.drawCenteredString(ms, fontrenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
            
            if(this.isHovered() && this.active)
            {
                DuelingDuelScreen.renderHoverRect(ms, this.x, this.y, this.width, this.height);
                this.renderToolTip(ms, mouseX, mouseY);
            }
        }
    }
    
    public static class ViewCardStackWidget extends Button
    {
        public final IDuelScreenContext context;
        public DuelCard hoverCard;
        protected int cardsTextureSize;
        protected int rows;
        protected int columns;
        protected int currentRow;
        protected List<DuelCard> cards;
        protected boolean forceFaceUp;
        
        public ViewCardStackWidget(IDuelScreenContext context, int x, int y, int width, int height, ITextComponent title, Consumer<ViewCardStackWidget> onPress, ITooltip onTooltip)
        {
            super(x, y, width, height, title, (button) -> onPress.accept((ViewCardStackWidget)button), onTooltip);
            this.context = context;
            this.hoverCard = null;
            this.rows = 0;
            this.columns = 0;
            this.currentRow = 0;
            this.deactivate();
        }
        
        public ViewCardStackWidget setRowsAndColumns(int cardsTextureSize, int rows, int columns)
        {
            this.cardsTextureSize = cardsTextureSize;
            this.rows = Math.max(1, rows);
            this.columns = Math.max(1, columns);
            return this;
        }
        
        public void activate(List<DuelCard> cards, boolean forceFaceUp)
        {
            this.active = true;
            this.visible = true;
            this.currentRow = 0;
            this.cards = cards;
            this.forceFaceUp = forceFaceUp;
        }
        
        public void forceFaceUp()
        {
            this.forceFaceUp = true;
        }
        
        public void deactivate()
        {
            this.cards = null;
            this.visible = false;
            this.active = false;
        }
        
        public int getCurrentRow()
        {
            return this.currentRow;
        }
        
        public int getMaxRows()
        {
            if(this.cards != null && this.columns > 0)
            {
                return MathHelper.ceil(this.cards.size() / (float)this.columns);
            }
            else
            {
                return 0;
            }
        }
        
        public void decreaseCurrentRow()
        {
            this.currentRow = Math.max(0, this.currentRow - 1);
        }
        
        public void increaseCurrentRow()
        {
            this.currentRow = Math.min(this.getMaxRows(), this.currentRow + 1);
        }
        
        @Override
        public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partialTicks)
        {
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer fontrenderer = minecraft.fontRenderer;
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            
            if(!this.cards.isEmpty())
            {
                this.hoverCard = this.renderCards(ms, mouseX, mouseY);
            }
            else
            {
                this.hoverCard = null;
            }
            
            int j = this.getFGColor();
            AbstractGui.drawCenteredString(ms, fontrenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
        }
        
        @Nullable
        public DuelCard renderCards(MatrixStack ms, int mouseX, int mouseY)
        {
            DuelCard hoveredCard = null;
            int hoverX = 0, hoverY = 0;
            
            int index = this.currentRow * this.columns;
            int x, y;
            DuelCard c;
            
            for(int i = 0; i < this.rows; ++i)
            {
                for(int j = 0; j < this.columns && index < this.cards.size(); ++j)
                {
                    x = this.x + j * this.cardsTextureSize;
                    y = this.y + i * this.cardsTextureSize;
                    
                    c = this.cards.get(index++);
                    
                    if(this.drawCard(ms, c, x, y, this.cardsTextureSize, this.cardsTextureSize, mouseX, mouseY))
                    {
                        hoverX = x;
                        hoverY = y;
                        hoveredCard = c;
                    }
                }
            }
            
            if(hoveredCard != null)
            {
                if(hoveredCard.getCardPosition().isFaceUp || this.forceFaceUp)
                {
                    this.context.renderCardInfo(ms, hoveredCard);
                }
                
                DuelingDuelScreen.renderHoverRect(ms, hoverX, hoverY, this.cardsTextureSize, this.cardsTextureSize);
            }
            
            if(!this.active)
            {
                return null;
            }
            else
            {
                return hoveredCard;
            }
        }
        
        protected boolean drawCard(MatrixStack ms, DuelCard duelCard, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY)
        {
            if(duelCard == this.context.getClickedDuelCard())
            {
                DuelingDuelScreen.renderSelectedRect(ms, renderX, renderY, renderWidth, renderHeight);
            }
            
            DuelingDuelScreen.renderCardCentered(ms, renderX, renderY, renderWidth, renderHeight, duelCard, this.forceFaceUp);
            
            if(this.isHovered() && mouseX >= renderX && mouseX < renderX + renderWidth && mouseY >= renderY && mouseY < renderY + renderHeight)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}
