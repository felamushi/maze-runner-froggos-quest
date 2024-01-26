package de.tum.cit.ase.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Gdx;

public class HUD {
    private final Stage stage;
    private final Image[] hearts;
    private final Image keyImage;
    private final TextureRegion keyTexture;
    private final TextureRegion noKeyTexture;
    float scaling = 2.0f;


    public HUD(TextureRegion fullHeart, TextureRegion emptyHeart, TextureRegion keyTexture, TextureRegion noKeyTexture, int initialLives, float timer) {
        this.keyTexture = keyTexture;
        this.noKeyTexture = noKeyTexture;






        stage = new Stage(new ScreenViewport());
        Table leftTable = new Table();
        Table rightTable = new Table();
        // Set up the left table for hearts
        leftTable.top().left();
        hearts = new Image[initialLives];
        for (int i = 0; i < initialLives; i++) {
            hearts[i] = new Image(fullHeart);
            hearts[i].setScale(scaling);// Set the size of the heart images
            leftTable.add(hearts[i]).pad(25);
        }

        // Set up the right table for the key image
        rightTable.top().right();
        keyImage = new Image(noKeyTexture);
        keyImage.setScale(scaling);// Set the size of the key image
        rightTable.add(keyImage).pad(25);

        // Add both tables to the stage
        stage.addActor(leftTable);
        stage.addActor(rightTable);
        leftTable.setFillParent(true);
        rightTable.setFillParent(true);


    }


    public void updateHearts(int currentLives, TextureRegion fullHeart, TextureRegion emptyHeart) {
        for (int i = 0; i < hearts.length; i++) {
            hearts[i].setDrawable(new TextureRegionDrawable(i < currentLives ? fullHeart : emptyHeart));
        }
    }

    public void updateKey(boolean hasKey) {
        keyImage.setDrawable(new TextureRegionDrawable(hasKey ? keyTexture : noKeyTexture));
    }
    public void updateExit(boolean reachedExit) {
        if(reachedExit){
            Label exitLabel = new Label("Exit reached", new Label.LabelStyle(new BitmapFont(), Color.WHITE));
            exitLabel.setFontScale(2.0f);
            stage.addActor(exitLabel);
            exitLabel.setPosition(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        }
    }

    public void draw() {
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }
}
