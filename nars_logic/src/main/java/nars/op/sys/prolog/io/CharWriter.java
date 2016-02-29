package nars.op.sys.prolog.io;

import nars.op.sys.prolog.terms.Int;
import nars.op.sys.prolog.terms.PTerm;
import nars.op.sys.prolog.terms.Prog;
import nars.op.sys.prolog.terms.Sink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer
 */
public class CharWriter extends Sink {
	@Nullable
	protected Writer writer;

	public CharWriter(@NotNull String f, Prog p) {
		super(p);
		this.writer = IO.toFileWriter(f);
	}

	public CharWriter(Prog p) {
		super(p);
		this.writer = IO.output;
	}

	@Override
	public int putElement(@NotNull PTerm t) {
		if (null == writer)
			return 0;
		try {
			char c = (char) ((Int) t).intValue();
			writer.write(c);
		} catch (IOException e) {
			return 0;
		}
		return 1;
	}

	@Override
	public void stop() {
		if (null != writer && IO.output != writer) {
			try {
				writer.close();
			} catch (IOException e) {
			}
			writer = null;
		}
	}
}
