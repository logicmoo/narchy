package nars.op.sys.prolog;

import nars.op.sys.prolog.builtins.Builtins;
import nars.op.sys.prolog.io.Parser;
import nars.op.sys.prolog.terms.PTerm;
import org.jetbrains.annotations.NotNull;

/**
 * AXR Axiomatic Reasoner - deterministic, programmable, mostly reliable
 * counterpart to NAR
 */
public class AXR extends Prolog {

	public AXR() {

		dict = new Builtins(this);
		ask("reconsult('"
				+ AXR.class.getClassLoader().getResource(Prolog.default_lib)
						.toExternalForm() + "')");
	}

	public static void main(String args[]) {
		new AXR().run(args).standardTop();
		// if(0==init())
		// return;
		// if(!Init.run(args))
		// return;
		// Init.standardTop(); // interactive
	}

	/** introduce a fact */
	@NotNull
	public PTerm add(String factString) {
		return db.add( Parser.stringToClause(this, factString) );
	}
}
