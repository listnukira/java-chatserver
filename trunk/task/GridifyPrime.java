package task;

import java.util.Vector;

public class GridifyPrime implements Task {
	public long prime = 379;
	public long min = 2;
	public long max = 378;
	
	public GridifyPrime() {}
	
	public GridifyPrime(String init_str) {
		this.init(init_str);
	}
	
	@Gridify(mapper="primeMapper", reducer="primeReducer")
	public Object execute() {
		if (min < 2)
			min = 2;
		for (long divisor = min; divisor < max; divisor++) {
			if (prime % divisor == 0) {
				return divisor;
			}
		}
		return 1L;
	}
	
	public Vector<GridifyPrime> primeMapper(int num) {
		Vector<GridifyPrime> tasks = new Vector<GridifyPrime>(num);
		long diff = (max - min + 1) / num;
		long start = min;
		long end = min + diff - 1;
		
		for (int i = 0; i < num; ++i) {
			if (end > max || i + 1 == num)
				end = max;
			tasks.add(new GridifyPrime(String.format("%d %d %d", prime, start, end)));
			start = end + 1;
			end = start + diff - 1;
		}
		return tasks;
	}
	
	public Long primeReducer(Vector<Long> mapped_results) {
		for (Long result : mapped_results) {
			if (result != 1) return result;
		}
		return 1L;
	}
	
	@Override
	public void init(String init_str) {
		try {
			String[] args = init_str.split("( )+", 3);
			prime = Long.valueOf(args[0]);
			min = Long.valueOf(args[1]);
			max = Long.valueOf(args[2]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}