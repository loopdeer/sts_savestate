package savestate;

import basemod.ClickableUIElement;
import basemod.patches.com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue.Save;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.jcraft.jogg.Page;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class SaveStateController {
    private static final String OPTION_KEY = "num_save_states";

    private static final float X_POSITION = 10F * Settings.scale;
    private static final float Y_POSITION_TOP = (Settings.HEIGHT - Settings.HEIGHT / 5);

    private static final Texture CONTROLLER_BACKGROUND = new Texture("ui/confirm.png");

    private static final Texture SAVE_TEXTURE = new Texture("ui/savestate.png");
    private static final Texture LOAD_TEXTURE = new Texture("ui/loadstate.png");

    private static final Texture NEXT_TEXTURE = new Texture("ui/right.png");
    private static final Texture PREV_TEXTURE = new Texture("ui/left.png");
    private static final Texture REDO_TEXTURE = new Texture("ui/redo.png");
    private static final Texture LAST_TURN_TEXTURE = new Texture("ui/stopwatch.png");

    private static final float STATE_PANEL_HEIGHT = Math
            .max(SAVE_TEXTURE.getHeight(), LOAD_TEXTURE.getHeight()) * 1.25f * Settings.scale;

    private static final float SAVE_BUTTON_X = X_POSITION + SAVE_TEXTURE
            .getWidth() * .5F * Settings.scale;
    private static final float LOAD_BUTTON_X = SAVE_BUTTON_X + SAVE_TEXTURE
            .getWidth() * 1.2F * Settings.scale;
    private static final float REDO_BUTTON_X = LOAD_BUTTON_X + REDO_TEXTURE.getWidth() * 1.2f * Settings.scale;
    private static final float LAST_TURN_X = REDO_BUTTON_X + LAST_TURN_TEXTURE.getWidth() * 1.2f * Settings.scale;

    private static final float PANEL_WIDTH = 350F * Settings.scale;
    public int currentLoadedIndex;
    public ArrayList<SaveState> undoList;

    private SaveState[] savedStates;
    private StatePanel[] statePanels;

    private SaveState lastTurn;
    private SaveState currentTurn;

    private PageStatePanel pages;

    public static int numSaveStates = 0;

    public static int visible_starting_index = 0;
    public static int hard_coded_visible_size = 4;
    public void initialize() {
        savedStates = new SaveState[numSaveStates];
        statePanels = new StatePanel[hard_coded_visible_size];
        currentLoadedIndex = -1;
        undoList = new ArrayList<>();
        pages = new PageStatePanel();
        updateStatePanels();
    }

    private void updateStatePanels() {
        int start_index = visible_starting_index;
        int end_index = visible_starting_index + hard_coded_visible_size;

        for (int i = start_index; i < end_index; i++) {
            if (i < savedStates.length)
                statePanels[i % hard_coded_visible_size] = new StatePanel(i);
            else
                statePanels[i % hard_coded_visible_size] = null;
        }
    }

    public void update() {
        if (savedStates != null && statePanels != null && pages != null) {
            for (int i = 0; i < statePanels.length; i++) {
                if (statePanels[i] != null){
                    statePanels[i].update();
                }
            }
            pages.update();
        }
    }

    public void render(SpriteBatch sb) {
        if (inCombat() && !SaveStateMod.shouldGoFast && savedStates != null) {
            sb.setColor(Color.WHITE);
            // Render Panel Background
            float height = (hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT;

            sb.draw(CONTROLLER_BACKGROUND, X_POSITION, Y_POSITION_TOP - height, PANEL_WIDTH, height);
            int end_index = visible_starting_index + hard_coded_visible_size;
            if (end_index > statePanels.length)
                end_index = statePanels.length;

            for (int i = 0; i < end_index; i++) {
                if (statePanels[i] != null)
                    statePanels[i].render(sb);
            }
            pages.render(sb);

        }
    }

    public static void saveNumSaveStates(int numSaveStates) {
        SpireConfig config = SaveStateMod.optionsConfig;
        if (config != null) {
            SaveStateController.numSaveStates = numSaveStates;
            config.setInt(OPTION_KEY, numSaveStates);
            try {
                config.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getNumSaveStates() {
        SpireConfig config = SaveStateMod.optionsConfig;
        if (config != null && config.has(OPTION_KEY)) {
            return config.getInt(OPTION_KEY);
        }
        return 0;
    }

    public void saveInNextEmptySlotOrOldest(){
        undoList.add(new SaveState());
        currentLoadedIndex = undoList.size() - 1;
    }

    private static boolean inCombat() {
        return CardCrawlGame.isInARun() && AbstractDungeon.currMapNode != null && AbstractDungeon
                .getCurrRoom() != null && AbstractDungeon
                .getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT;
    }

    class StatePanel {
        private final int saveIndex;
        private final SaveButton saveButton;
        private final LoadButton loadButton;

        public StatePanel(int saveIndex) {
            this.saveIndex = saveIndex;

            this.saveButton = new SaveButton(saveIndex);
            this.loadButton = new LoadButton(saveIndex);
        }

        public void render(SpriteBatch spriteBatch) {
            saveButton.render(spriteBatch);
            loadButton.render(spriteBatch);

            float textX = LOAD_BUTTON_X + LOAD_TEXTURE.getWidth() * 1.2F * Settings.scale;
            float textY = Y_POSITION_TOP - ((saveIndex % hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                    STATE_PANEL_HEIGHT * .70F;

            FontHelper
                    .renderFont(spriteBatch, FontHelper.tipBodyFont, stateString(savedStates[saveIndex]), textX, textY, Settings.GREEN_TEXT_COLOR);
        }

        public void update() {
            saveButton.update();
            loadButton.update();
        }

        public SaveState getState() {
            return savedStates[saveIndex];
        }

        class SaveButton extends ClickableUIElement {
            public SaveButton(int index) {
                super(SAVE_TEXTURE);

                x = SAVE_BUTTON_X;
                y = Y_POSITION_TOP - ((index % hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        SAVE_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, SAVE_TEXTURE.getWidth(), SAVE_TEXTURE.getHeight());
            }

            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                savedStates[saveIndex] = new SaveState();
            }
        }

        class LoadButton extends ClickableUIElement {
            public LoadButton(int index) {
                super(LOAD_TEXTURE);

                x = LOAD_BUTTON_X;
                y = Y_POSITION_TOP - ((index % hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        LOAD_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, LOAD_TEXTURE.getWidth(), LOAD_TEXTURE.getHeight());
            }

            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                if (savedStates[saveIndex] != null) {
                    savedStates[saveIndex].loadState();
                }
            }
        }
    }

    class PageStatePanel {
        private final PreviousButton prevButton;
        private final NextButton nextButton;
        private final RedoButton redoButton;
        // private final LastTurnButton lastTurnButton;

        public PageStatePanel() {
            this.prevButton = new PreviousButton();
            this.nextButton = new NextButton();
            this.redoButton = new RedoButton();
            // this.lastTurnButton = new LastTurnButton();
        }

        public void render(SpriteBatch spriteBatch) {
            prevButton.render(spriteBatch);
            nextButton.render(spriteBatch);
            redoButton.render(spriteBatch);
            // lastTurnButton.render(spriteBatch);
        }

        public void update() {
            prevButton.update();
            nextButton.update();
            redoButton.update();
            // lastTurnButton.update();
        }
        class PreviousButton extends ClickableUIElement{
            public PreviousButton() {
                super(PREV_TEXTURE);

                x = SAVE_BUTTON_X;
                y = Y_POSITION_TOP - ((hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        PREV_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, PREV_TEXTURE.getWidth(), PREV_TEXTURE.getHeight());
            }
            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                currentLoadedIndex -= 1;
                if (currentLoadedIndex >= 0) {
                    undoList.get(currentLoadedIndex).loadState();
                }
                else {
                    currentLoadedIndex = 0;
                }
            }
        }
        class NextButton extends ClickableUIElement {
            public NextButton() {
                super(NEXT_TEXTURE);

                x = LOAD_BUTTON_X;
                y = Y_POSITION_TOP - ((hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        NEXT_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, NEXT_TEXTURE.getWidth(), NEXT_TEXTURE.getHeight());
            }

            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                currentLoadedIndex += 1;
                if (currentLoadedIndex < undoList.size()) {
                    undoList.get(currentLoadedIndex).loadState();
                }
                else {
                    currentLoadedIndex = undoList.size() - 1;
                }
            }
        }
        class RedoButton extends ClickableUIElement {
            public RedoButton() {
                super(REDO_TEXTURE);

                x = REDO_BUTTON_X;
                y = Y_POSITION_TOP - ((hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        REDO_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, REDO_TEXTURE.getWidth(), REDO_TEXTURE.getHeight());
            }

            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                undoList.get(currentLoadedIndex).loadState();
            }
        }
        class LastTurnButton extends ClickableUIElement {
            public LastTurnButton() {
                super(LAST_TURN_TEXTURE);

                x = LAST_TURN_X;
                y = Y_POSITION_TOP - ((hard_coded_visible_size + 1) * STATE_PANEL_HEIGHT) +
                        LAST_TURN_TEXTURE.getHeight() / 8F;

                hitbox = new Hitbox(x, y, LAST_TURN_TEXTURE.getWidth(), LAST_TURN_TEXTURE.getHeight());
            }

            @Override
            protected void onHover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 15.0F);
                this.tint.a = 0.25F;
            }

            @Override
            protected void onUnhover() {
                this.angle = MathHelper.angleLerpSnap(this.angle, 0.0F);
                this.tint.a = 0.0F;
            }

            @Override
            protected void onClick() {
                if (lastTurn != null)
                    lastTurn.loadState();
            }
        }
    }

    private static String stateString(SaveState saveState) {
        if (saveState == null) {
            return "(empty)";
        }
        return String
                .format("Turn %02d \t Energy %d/%d", saveState.turn, saveState.playerState.energyPanelTotalEnergy, saveState.playerState.energyManagerMaxMaster);
    }
}
