import java.io.*;
import java.util.*;

class NFA {
	public static StateMachine nfa;
	public static HashMap<Integer, HashMap<Character, Set<Integer>>> cache;

	public static void main(String[] args) {
		String inputNFA = args[0];
		String strings = args[1];
		InputParser inputParser = new InputParser();
		nfa = inputParser.parseFile(inputNFA);
		cacheDFAStates();

		nfa.print(true);
		StateMachine dfa = convertToDFA();
		System.out.println("\n-----------------------------------------------------------------------------\n");
		dfa.print(false);

		System.out.println("Parsing results of strings in strings.txt:");
		for (String input : getStrings(strings)) {
			if (input != null && isValidForString(dfa, input)) {
				System.out.print("True ");
			} else {
				System.out.print("False ");
			}
		}
	}

	public static ArrayList<String> getStrings(String inputFilename) {
		ArrayList<String> out = new ArrayList<>();
		File strings = new File(inputFilename);
		Scanner scan = null;
		try {
			scan = new Scanner(strings);
		} catch (Exception e) {

		}
		
		while(scan.hasNext()) {

			out.add(scan.nextLine());
			
		} 
		return out;
	}
  
	public static boolean isValidForString(StateMachine dfa, String input) {
		State currentState = dfa.states.get(dfa.initialLabel);

		for (int i = 0; i < input.length(); i++) {
			char currentInput = input.charAt(i);
			if (!dfa.validInputs.contains(currentInput)) {
				return false;
			}
			Object[] transitions = currentState.transitionMap.get(currentInput).toArray();
			int currentStateLabel = (Integer)transitions[0];
			currentState = dfa.states.get(currentStateLabel);
		}

		
		if (currentState.accepting) {
			return true;
		}
		return false;
	}

	public static StateMachine convertToDFA() {
		StateMachine DFA = new StateMachine();
		State initial = new State();
		initial.label = 0;
		DFA.addState(initial);
		DFA.initialLabel = 0;
		DFA.validInputs = nfa.validInputs;

		Set<Integer> initialState = new HashSet<>();
		initialState.add(nfa.initialLabel);

		Set<Set<Integer>> seen = new HashSet<>();
		seen.add(initialState);

		Queue<Set<Integer>> queue = new LinkedList<>();
		queue.add(initialState);

		int labelI = 0;
		HashMap<Set<Integer>, Integer> dfaSetToLabel = new HashMap<>();
		dfaSetToLabel.put(initialState, labelI++);
		

		while (!queue.isEmpty()) {
			Set<Integer> curr = queue.remove();
			State currState = DFA.states.get(dfaSetToLabel.get(curr));

			for (char input : nfa.validInputs) {
				Set<Integer> newState = composeDFAState(curr, input);
				
				if (!seen.contains(newState)) {
					State state = new State();
					Set<Integer> intersection = new HashSet<>();
					intersection.addAll(nfa.acceptingLabels);
					intersection.retainAll(newState);
					state.label = labelI++;
					if (!intersection.isEmpty()) {
						state.accepting = true;
						DFA.acceptingLabels.add(state.label);
					}
					dfaSetToLabel.put(newState, state.label);
					
					seen.add(newState);
					queue.add(newState);
					DFA.addState(state);
				}

				HashSet<Integer> temp = new HashSet<>();
				temp.add(dfaSetToLabel.get(newState));
				currState.transitionMap.put(input, temp);
			}
		}

		return DFA;
	}


	public static void printCache() {
		for (int label : cache.keySet()) {
			for (char input : cache.get(label).keySet()) {
				System.out.println(cache.get(label).get(input).toString());
			}
		}
	}
	
	public static void cacheDFAStates() {
		HashMap<Integer, HashMap<Character, Set<Integer>>> cache = new HashMap<>();
		for (int label : nfa.states.keySet()) {
			HashMap<Character, Set<Integer>> stateToInput = new HashMap<>();
			for (char input : nfa.validInputs) {
				stateToInput.put(input, getDFAState(label, input));
			}
			cache.put(label, stateToInput);
		}
		
		NFA.cache = cache;
	}

	// Get the DFA State from the input of one state
	public static Set<Integer> getDFAState(int initialState, char input) {
		Set<Integer> newState = new HashSet<>();
		Queue<StateWrapper> queue = new LinkedList<>();
		queue.add(new StateWrapper(initialState));

		while (!queue.isEmpty()) {
			StateWrapper currStateWrap = queue.remove();
			State currState = nfa.states.get(currStateWrap.label);	

			// Lambda Transitions
			for (int label : currState.lambdaTransitions) {
				queue.add(new StateWrapper(label, currStateWrap.ticket));
			}

			if (currStateWrap.ticket) {
				if (currState.getTransitions(input) != null) {
					for (int state : currState.getTransitions(input)) {
						queue.add(new StateWrapper(state, false));	
					}
				}
				
			} else {
				newState.add(currState.label);
			}
			
		}

		return newState;
	}

	public static Set<Integer> composeDFAState(Set<Integer> states, char input) {
		Set<Integer> newState = new HashSet<>();
		for (int label : states) {
			newState.addAll(cache.get(label).get(input));
		}

		return newState;
	} 
}

class InputParser {
	public StateMachine parseFile(String fileName) {
		ArrayList<String> input = readLines(new File(fileName));
		return createStateMachine(input);
	}

	public ArrayList<String> readLines(File input) {
		Scanner reader = null;
		//first two last two known Input 
		ArrayList<String> inputLines = new ArrayList<>();
		
		try {
			reader = new Scanner(input);	
		} catch (Exception e) {
			System.out.println("File Not Found!");	
		}

		while (reader.hasNext()) {
			inputLines.add(reader.nextLine());
		}

		return inputLines;
	}
	
	public StateMachine createStateMachine(ArrayList<String> smInput) {
		StateMachine newSM = new StateMachine();
		
		int numStates = Integer.parseInt(smInput.get(0));
		formatValidInputs(newSM, smInput.get(1));
		formatAcceptingStates(newSM, smInput.get(smInput.size() - 1));
		newSM.initialLabel = Integer.parseInt(smInput.get(smInput.size() - 2));
		formatStates(newSM, smInput);
		
		return newSM;
		
	}

	private void formatStates(StateMachine sm, ArrayList<String> inputText) {

		for (int i = 2; i < inputText.size() - 2; i++) {
			State state = new State();
			String curr = inputText.get(i);
			curr = curr.trim();
			String[] transitions = curr.split(" ");

			state.label = Integer.parseInt(transitions[0].replace(":", ""));
			for (int j = 1; j < transitions.length- 1; j++) {
				String toLabel = transitions[j].replace("{","").replace("}","");
				if (toLabel.length() > 0) {
					String[] currTransitions = toLabel.split(",");
					HashSet<Integer> transitionSet = new HashSet<>();
					for (String label : currTransitions) {
						transitionSet.add(Integer.parseInt(label));
					}
					state.addConnection((char)('a' + j - 1), transitionSet);
				} 
			} 
			
			String lambdas = transitions[transitions.length - 1];
			lambdas = lambdas.replace("{", "").replace("}", "");
			String[] lambdaTransitions = lambdas.split(",");

			for (int j = 0; j < lambdaTransitions.length; j++) {
				if (lambdaTransitions[j].length() > 0) {
					state.lambdaTransitions.add(Integer.parseInt(lambdaTransitions[j]));
				} 
				else {
					break;
				}
			}

			sm.addState(state);
		}
	}
	
	private void formatValidInputs(StateMachine sm, String validInputString) {
		String[] splitInputs = validInputString.split(" ");
		for (String input : splitInputs) {
			sm.validInputs.add(input.charAt(0));
		}
	}
	
	private void formatAcceptingStates(StateMachine sm, String acceptingStateString) {
		acceptingStateString = acceptingStateString.substring(1, acceptingStateString.length() - 1);
		String[] splitInputs = acceptingStateString.split(",");
		for (String input :splitInputs) {
			sm.acceptingLabels.add(Integer.parseInt(input));
		}	
	}
}


class State {
  public HashMap<Character, HashSet<Integer>> transitionMap = new HashMap<>(); 
  public HashSet<Integer> lambdaTransitions = new HashSet<>();
  public boolean accepting = false;
  int label;

  public void addConnection(char input, HashSet<Integer> transitions) {
    transitionMap.put(input, transitions);
  }

  // -2 means no state, -1 means lambda
  public HashSet<Integer> getTransitions(char input) {
    if (transitionMap.containsKey(input)) {
      return transitionMap.get(input);
    } else {
      return new HashSet<Integer>();
    }
  }
  
  public void printTransitions() {
  	System.out.println(Arrays.asList(transitionMap));
  }
}

class StateMachine {
    public HashMap<Integer, State> states = new HashMap<>();
    public Set<Integer> acceptingLabels = new HashSet<>();
    public Set<Character> validInputs = new HashSet<>();
    public int initialLabel;

    public void addState(State s) {
        states.put(s.label, s);
    }

    public HashSet<Integer> transition(int fromLabel, char input) {
        if (states.containsKey(fromLabel)) {
            ArrayList<State> transitions = new ArrayList<>();
            return states.get(fromLabel).getTransitions(input);
        } 

        return null;
    }
    
    public void print(boolean isNFA) {
    	System.out.println("Sigma: ");
    	ArrayList<Character> sigma = new ArrayList<>(validInputs);
		Collections.sort(sigma);
		ArrayList<Integer> stateLabels = new ArrayList<>(states.keySet());
		Collections.sort(stateLabels);
		
		System.out.printf("%3s", "");
    	for (int i = 0; i < sigma.size(); i++) {
    		System.out.printf("%20c", sigma.get(i));
    	}
    	
    	if (isNFA) {
    		System.out.printf("%20c", 'L');
    	}
    	
    	System.out.println("\n---------------------------------------------------------------------------------------------");
    	
    	for (int i = 0; i < stateLabels.size(); i++) {
    		System.out.printf("%3d:", stateLabels.get(i));
    		State currState = states.get(stateLabels.get(i));
    		for (int j = 0; j < sigma.size(); j++) {
    			HashSet<Integer> nextState = currState.getTransitions(sigma.get(j));
    			if (nextState.size() == 0) {
    				System.out.printf("%20c", '/');
    			} else {
    				System.out.printf("%20s", nextState.toString());	
    			}
    			
    		}
    		if (isNFA) {
    			System.out.printf("%20s%s", "", currState.lambdaTransitions.toString());
    		}
    		System.out.println();
    	}
    	System.out.println("---------------------------------------------------------------------------------------------");
    	System.out.printf("Initial State: %d\n", initialLabel);
    	System.out.printf("Accepting State(s): %s\n", acceptingLabels.toString());
    }
}

class StateWrapper {
	public int label;
	public boolean ticket = true;

	public StateWrapper(int label) {
		this.label = label;
	}

	public StateWrapper(int label, boolean ticket) {
		this.label = label;
		this.ticket = ticket;
	}
}