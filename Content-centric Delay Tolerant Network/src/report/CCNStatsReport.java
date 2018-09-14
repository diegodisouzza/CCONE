package report;

import projeto.Registers;

public class CCNStatsReport extends Report {

	private String header = "interval"+
			"|ads made"+
			"|interests created"+
			"|interests sentout"+
			"|interests delivered"+
			"|interests relayed"+
			"|satisfied on PIT"+
			"|satisfied on requester"+
			"|contents sentout"+
			"|contents delivered"+
			"|contents cached"+
			"|relayers"+
			"|avg interest relayed per node"+
			"|dp interest relayed per node"+
			"|avg delay"+
			"|satisfaction rate";
	@Override
	public void done() {
		write(header);
		
		String statsText = Registers.register;
		write(statsText);
		super.done();
	}
}
