package fr.loria.parole.artimate.engine;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitSequence;

public interface GameEngineWrapper {

	public void copyAnimation(Unit source, Unit target);

	public void playAnimation(UnitSequence units);

}
