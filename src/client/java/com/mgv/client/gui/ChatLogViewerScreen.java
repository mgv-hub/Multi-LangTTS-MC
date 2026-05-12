package com.mgv.client.gui;

import com.mgv.client.chat.ChatLogger;
import com.mgv.client.CoreModClient;
import com.mgv.client.tts.util.ArabicTextUtil;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.format.DateTimeFormatter;
import java.util.List;

// ChatLogViewerScreen: scrollable list of today's chat entries with TTS replay
public class ChatLogViewerScreen extends Screen {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Screen parent;
    private final ChatLogger chatLogger;
    private List<ChatLogger.ChatEntry> entries;
    private float scrollOffset;
    private int contentHeight;
    private boolean isDraggingScrollbar = false;
    // layout constants - tuned for readability on 1080p+ displays
    private final int itemHeight = 26;
    private final int listWidth = 360;
    private final int playBtnWidth = 70;
    
    public ChatLogViewerScreen(Screen parent, ChatLogger chatLogger) {
        super(Text.translatable("text.multilangtts.chat_viewer.title"));
        this.parent = parent;
        this.chatLogger = chatLogger;
    }
    
    @Override
    protected void init() {
        entries = chatLogger.loadTodayLogs();
        contentHeight = entries.size() * itemHeight;
        scrollOffset = 0;
        rebuildWidgets();
    }
    
    // rebuilds all buttons on scroll/config change - simple but effective for <500 entries
    private void rebuildWidgets() {
        clearChildren();
        
        // Back button - standard placement at bottom center
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.back"),
            b -> client.setScreen(parent)
        ).dimensions(width / 2 - 100, height - 32, 200, 20).build());
        
        int listLeft = (width - listWidth) / 2;
        int listTop = 30;
        int maxVisible = (height - 70) / itemHeight;
        int startIdx = Math.max(0, (int) (scrollOffset / itemHeight));
        int endIdx = Math.min(entries.size(), startIdx + maxVisible + 1);
        
        for (int i = startIdx; i < endIdx; i++) {
            ChatLogger.ChatEntry entry = entries.get(i);
            int y = listTop + (i * itemHeight) - (int) scrollOffset;
            
            // cull off-screen items early to save widget overhead
            if (y < 20 || y > height - 50) continue;
            
            // strip bidi markers for clean display - original message preserved for TTS
            String cleanMessage = ArabicTextUtil.stripBidiMarkers(entry.message());
            String cleanSender = ArabicTextUtil.stripBidiMarkers(entry.sender());
            
            String display = String.format("[%s] %s: %s",
                entry.time().format(TIME_FMT),
                cleanSender,
                cleanMessage
            );
            
            // Message text button - clicking replays via TTS
            addDrawableChild(ButtonWidget.builder(
                Text.literal(display),
                btn -> CoreModClient.getTtsEngine().enqueue(entry.message())
            ).dimensions(listLeft, y, listWidth - playBtnWidth - 6, 20).build());
            
            // Play button - explicit replay control, separate from text click
            addDrawableChild(ButtonWidget.builder(
                Text.literal("\u25B6"),
                btn -> CoreModClient.getTtsEngine().enqueue(entry.message())
            ).dimensions(listLeft + listWidth - playBtnWidth, y, playBtnWidth, 20).build());
        }

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (contentHeight > height - 70) {
            scrollOffset = MathHelper.clamp(
                scrollOffset - (float) (verticalAmount * 18),
                0,
                Math.max(0, contentHeight - (height - 70))
            );
            rebuildWidgets();
            return true;
        }
        return false;
    }

    // detect initial click on the scrollbar thumb to start drag operation
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (contentHeight <= height - 70) return super.mouseClicked(click, doubled);

        int listLeft = (width - listWidth) / 2;
        int scrollBarX = listLeft + listWidth + 10;
        int trackHeight = height - 70;
        int thumbHeight = Math.max(20, (trackHeight * trackHeight) / contentHeight);
        int thumbY = 30 + (int) ((scrollOffset / contentHeight) * trackHeight);

        if (mouseX >= scrollBarX && mouseX <= scrollBarX + 4 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
            isDraggingScrollbar = true;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    // map vertical mouse movement to scroll offset while dragging
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (isDraggingScrollbar && contentHeight > height - 70) {
            int trackHeight = height - 70;
            int listLeft = (width - listWidth) / 2;
            int scrollBarX = listLeft + listWidth + 10;

            // only process if mouse is near the scrollbar track
            if (mouseX >= scrollBarX - 5 && mouseX <= scrollBarX + 9) {
                float ratio = (float) (mouseY - 30) / trackHeight;
                scrollOffset = MathHelper.clamp(ratio * (contentHeight - trackHeight), 0, contentHeight - trackHeight);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    // release drag state when left mouse button is lifted
    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(click);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int listLeft = (width - listWidth) / 2;
        
        // Title - centered, standard shadow for readability
        context.drawCenteredTextWithShadow(
            client.textRenderer,
            title,
            width / 2,
            7,
            0xFFFFFF
        );
        
        // List background - subtle dark overlay to separate from main screen
        context.fill(listLeft - 6, 25, listLeft + listWidth + 6, height - 40, 0x40101010);
        
        // Scrollbar - only shown when content overflows
        if (contentHeight > height - 70) {
            int scrollBarHeight = height - 70;
            int thumbHeight = Math.max(20, (scrollBarHeight * scrollBarHeight) / contentHeight);
            int scrollBarX = listLeft + listWidth + 10;
            int thumbY = 30 + (int) ((scrollOffset / contentHeight) * scrollBarHeight);
            
            context.fill(scrollBarX, 30, scrollBarX + 4, height - 40, 0x30FFFFFF);
            context.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0x90FFFFFF);
        }
    }
}