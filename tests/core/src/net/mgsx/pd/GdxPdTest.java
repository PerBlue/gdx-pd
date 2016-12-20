package net.mgsx.pd;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.mgsx.pd.midi.MidiMusic;

public class GdxPdTest extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;
	MidiMusic music;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");
		
		Pd.audio.create(new PdConfiguration());
		//Pd.audio.open(Gdx.files.internal("pdmidi/midiplayer.pd"));
		Pd.audio.open(Gdx.files.internal("pdmidi/midiplayer.pd"));
		//Pd.audio.open(Gdx.files.internal("pd/test.pd"));
		
		if(Pd.midi != null){
			music = Pd.midi.createMidiMusic(Gdx.files.internal("MuteCity.mid"));
			music.play();
		}
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		batch.draw(img, 0, 0);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
		music.dispose();
		Pd.audio.release();
	}
}