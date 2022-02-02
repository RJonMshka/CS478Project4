package com.example.cs478project4;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Constants
    public final int GAME_CMD_START = 1;
    public final int GAME_CMD_INIT = 3;
    public final int GAME_CMD_PLAY = 4;
    public final int GAME_STATUS_P1_INITIATED = 5;
    public final int GAME_STATUS_P2_INITIATED = 6;
    public final int GAME_STATUS_P1_PLAYED = 7;
    public final int GAME_STATUS_P2_PLAYED = 8;
    public final int GAME_STATUS_P1_RESPONDED = 9;
    public final int GAME_STATUS_P2_RESPONDED = 10;
    public final int P1_UPDATE_UI = 11;
    public final int P2_UPDATE_UI = 12;
    public final int GAME_STATUS_P1_WON = 13;
    public final int GAME_STATUS_P2_WON = 14;
    public final int HALT_PLAY = 20;

    // Initiation variables for both players
    boolean p1Initiated = false;
    boolean p2Initiated = false;
    boolean bothInitiated = false;
    boolean gameStarted = false;

    // Thread rest time to replicate human thinking time
    public final int REST_TIME = 1500;
    // Maximum Number of games accepted
    public final int gameLimit = 40;

    // Size of number that players have chosen
    public int gameSize = 4;

    // variables to hold chosen number
    protected String p1chosenNumber;
    protected String p2chosenNumber;

    // variables to hold current guesses of each player
    protected String p1CurrentGuess;
    protected String p2CurrentGuess;

    // Stores number of games played
    protected int numberOfGamesPlayed;

    // List of all the previous guesses, each for each player thread
    protected ArrayList<String> p1PrevGuesses = new ArrayList<String>();
    protected ArrayList<String> p2PrevGuesses = new ArrayList<String>();
    // Lists to store numbers that are correctly guessed by each player
    protected ArrayList<String> p1CorrectGuessedNumbers = new ArrayList<String>();
    protected ArrayList<String> p2CorrectGuessedNumbers = new ArrayList<String>();
    // variables to hold hints for player 1
    protected Integer p1RightPosMatches;
    protected Integer p1WrongPosMatches;
    protected Integer p1HintNumber;
    // variables to hold hints for player 2
    protected Integer p2RightPosMatches;
    protected Integer p2WrongPosMatches;
    protected Integer p2HintNumber;

    // Variables to hold thread reference
    private Thread p1Thread;
    private Thread p2Thread;

    // Variables to hold Thread Handler references
    public Handler UIThreadHandler;
    public Handler P1ThreadHandler;
    public Handler P2ThreadHandler;

    // Store loopers
    public Looper P1Looper;
    public Looper P2Looper;

    // All the views
    TextView p1NumberField;
    TextView p2NumberField;
    TextView p1GuessField;
    TextView p2GuessField;
    TextView p1Hint1Field;
    TextView p1Hint2Field;
    TextView p1HintMissingField;
    TextView p2Hint1Field;
    TextView p2Hint2Field;
    TextView p2HintMissingField;
    TextView gameStatus;
    Button startButton;
    RelativeLayout p1Container;
    RelativeLayout p2Container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting up the views
        p1Container = (RelativeLayout) findViewById(R.id.player1_section);
        p2Container = (RelativeLayout) findViewById(R.id.player2_section);

        p1NumberField = (TextView) findViewById(R.id.player1_number);
        p2NumberField = (TextView) findViewById(R.id.player2_number);

        p1GuessField = (TextView) findViewById(R.id.player1_guess_number);
        p2GuessField = (TextView) findViewById(R.id.player2_guess_number);

        p1Hint1Field = (TextView) findViewById(R.id.player1_hint_1_value);
        p1Hint2Field = (TextView) findViewById(R.id.player1_hint_2_value);
        p1HintMissingField = (TextView) findViewById(R.id.player1_hint_missing_digit);

        p2Hint1Field = (TextView) findViewById(R.id.player2_hint_1_value);
        p2Hint2Field = (TextView) findViewById(R.id.player2_hint_2_value);
        p2HintMissingField = (TextView) findViewById(R.id.player2_hint_missing_digit);
        gameStatus = (TextView) findViewById(R.id.game_status);

        startButton = (Button) findViewById(R.id.start_button);

        // set the initial text
        gameStatus.setText(R.string.g_not_started);

        // click listener for button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameStarted = !gameStarted;

                if(gameStarted) {
                    Message msg = UIThreadHandler.obtainMessage(GAME_CMD_START);
                    UIThreadHandler.sendMessage(msg);
                } else {
                    startButton.setText(R.string.start_btn_Text);
                    resetThreads();
                    resetGames();
                    resetData();
                    clearTextFields();
                    gameStatus.setText(R.string.game_stopped);
                }

            }
        });

        // initiating and starting the threads
        p1Thread = new Thread(new Player1Runnable());
        p2Thread = new Thread(new Player2Runnable());
        p1Thread.start();
        p2Thread.start();

        // UI Thread Handler definition
        UIThreadHandler = new Handler(getMainLooper()) {
            @SuppressLint("ResourceAsColor")
            @Override
            public void handleMessage(@NonNull Message msg) {

                Message message;

                switch (msg.what) {

                    // If message is to start the game, by clicking start game button
                    case GAME_CMD_START: {
                        // Set the text of game started
                        resetGames();
                        gameStatus.setText(R.string.g_started);
                        // change text of start button
                        startButton.setText(R.string.end_btn_Text);
                        // Clear text fields if they are not cleared previously
                        clearTextFields();

                        // Add a delay to P1 Thread
                        P1ThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(REST_TIME);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Send message to initialize P1
                        message = P1ThreadHandler.obtainMessage(GAME_CMD_INIT);
                        P1ThreadHandler.sendMessage(message);
                        // Add a delay to P2 Thread
                        P2ThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(REST_TIME);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        // Send message to initialize P2
                        message = P2ThreadHandler.obtainMessage(GAME_CMD_INIT);
                        P2ThreadHandler.sendMessage(message);

                        break;
                    }

                    // When P1 choses its number and is ready
                    case GAME_STATUS_P1_INITIATED: {
                        setP1Init(true);
                        p1NumberField.setText(p1chosenNumber);

                        // if both are initiated
                        if(p1Initiated && p2Initiated && !bothInitiated) {
                            setBothInit(true);
                            // Add a delay to P1 Thread
                            P1ThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(REST_TIME);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            // Start with player 1
                            message = P1ThreadHandler.obtainMessage(GAME_CMD_PLAY);
                            gameStatus.setText(R.string.p1_guessing);
                            p1Container.setBackgroundResource(R.color.teal_200);
                            p2Container.setBackgroundResource(R.color.white);

                            P1ThreadHandler.sendMessage(message);
                        }

                        break;
                    }

                    // When P2 is ready and choses its number already
                    case GAME_STATUS_P2_INITIATED: {
                        setP2Init(true);
                        p2NumberField.setText(p2chosenNumber);

                        // if both are initiated
                        if(p1Initiated && p2Initiated && !bothInitiated) {
                            setBothInit(true);
                            // Add a delay to P1 Thread
                            P1ThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(REST_TIME);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            message = P1ThreadHandler.obtainMessage(GAME_CMD_PLAY);
                            gameStatus.setText(R.string.p1_guessing);
                            p1Container.setBackgroundResource(R.color.teal_200);
                            p2Container.setBackgroundResource(R.color.white);
                            // Start with player 1
                            P1ThreadHandler.sendMessage(message);
                        }

                        break;
                    }

                    // Update the UI of P1 when it has guesses and P2 has responded back to it
                    case P1_UPDATE_UI: {
                        Bundle b = msg.getData();
                        String response = (String) msg.obj;
                        p1GuessField.setText(getP1GuessesList());
                        p1Hint1Field.setText( Character.toString(response.charAt(0)) );
                        p1Hint2Field.setText( Character.toString(response.charAt(1)) );
                        if(response.length() == 3) {
                            p1HintMissingField.setText( Character.toString(response.charAt(2)) );
                        } else {
                            p1HintMissingField.setText("-");
                        }

                        // check if p1 has won or not
                        if(Character.getNumericValue(response.charAt(0)) == gameSize) {
                            // it means all numbers are matching at right position - which means P1 has won
                            message = UIThreadHandler.obtainMessage(GAME_STATUS_P1_WON);
                            UIThreadHandler.sendMessage(message);

                        // if not won then check if limit has reached
                        } else if(getGamesPlayed() < gameLimit) {
                            // P2 guessing
                            gameStatus.setText(R.string.p2_guessing);
                            p2Container.setBackgroundResource(R.color.teal_200);
                            p1Container.setBackgroundResource(R.color.white);
                            // Add a delay to P2 Thread
                            P2ThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(REST_TIME);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            message = P2ThreadHandler.obtainMessage(GAME_CMD_PLAY);
                            P2ThreadHandler.sendMessage(message);

                        // if limit has reached, then halt the game
                        } else {
                            message = UIThreadHandler.obtainMessage(HALT_PLAY);
                            UIThreadHandler.sendMessage(message);
                        }
                        break;
                    }

                    // Update the UI of P2 when it has guesses and P1 has responded back to it
                    case P2_UPDATE_UI: {
                        Bundle b = msg.getData();
                        String response = (String) msg.obj;

                        p2GuessField.setText(getP2GuessesList());
                        p2Hint1Field.setText( Character.toString(response.charAt(0)) );
                        p2Hint2Field.setText( Character.toString(response.charAt(1)) );
                        if(response.length() == 3) {
                            p2HintMissingField.setText( Character.toString(response.charAt(2)) );
                        } else {
                            p2HintMissingField.setText("-");
                        }

                        // check if P2 has won or not
                        if(Character.getNumericValue(response.charAt(0)) == gameSize) {
                            // it means all numbers are matching at right position - which means P2 has won
                            message = UIThreadHandler.obtainMessage(GAME_STATUS_P2_WON);
                            UIThreadHandler.sendMessage(message);

                        // if not won then check if limit has reached
                        } else if(getGamesPlayed() < gameLimit) {
                            // P1 guessing
                            gameStatus.setText(R.string.p1_guessing);
                            p1Container.setBackgroundResource(R.color.teal_200);
                            p2Container.setBackgroundResource(R.color.white);

                            P1ThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(REST_TIME);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            message = P1ThreadHandler.obtainMessage(GAME_CMD_PLAY);
                            P1ThreadHandler.sendMessage(message);

                        // if limit has reached, then halt the game
                        } else {
                            message = UIThreadHandler.obtainMessage(HALT_PLAY);
                            UIThreadHandler.sendMessage(message);
                        }
                        break;
                    }

                    // When P1 has won
                    case GAME_STATUS_P1_WON: {
                        gameStatus.setText(R.string.p1_won);
                        p1Container.setBackgroundResource(R.color.white);
                        p2Container.setBackgroundResource(R.color.white);

                        resetData();
                        gameStarted = !gameStarted;
                        startButton.setText(R.string.start_btn_Text);
                        break;
                    }

                    // When P2 has won
                    case GAME_STATUS_P2_WON: {
                        gameStatus.setText(R.string.p2_won);
                        p1Container.setBackgroundResource(R.color.white);
                        p2Container.setBackgroundResource(R.color.white);

                        resetData();
                        gameStarted = !gameStarted;
                        startButton.setText(R.string.start_btn_Text);
                        break;
                    }

                    // Halting the game
                    case HALT_PLAY: {
                        // Game Halted
                        gameStatus.setText(R.string.game_halt);
                        p1Container.setBackgroundResource(R.color.white);
                        p2Container.setBackgroundResource(R.color.white);

                        resetData();
                        gameStarted = !gameStarted;
                        startButton.setText(R.string.start_btn_Text);
                        // Stop the threads and probably start them again
                        break;
                    }

                    default:
                        throw new IllegalStateException("Unexpected value: " + msg.what);
                }
            }
        };
    }

    // Player 1 Thread Runnable
    public class Player1Runnable implements Runnable {
        @Override
        public void run() {
            Looper.prepare();

            P1Looper = Looper.myLooper();

            // P1 Thread Handler definition
            P1ThreadHandler = new Handler(P1Looper) {
                @SuppressLint("NewApi")
                @Override
                public void handleMessage(@NonNull Message msg) {

                    Message message;

                    switch(msg.what) {
                        // Intialize P1 and chose a number
                        case GAME_CMD_INIT: {
                            p1chosenNumber = getRandomNumber();
                            // sending message back to UI thread that it has initialized
                            message = UIThreadHandler.obtainMessage(GAME_STATUS_P1_INITIATED);
                            UIThreadHandler.sendMessage(message);

                            break;
                        }

                        // Starts to guess
                        case GAME_CMD_PLAY: {
                            // Guessing the new number w.r.t previous response from the opponent
                            p1CurrentGuess = guessNumberForP1();
                            // ask for response from P2 when it has guessed
                            addGuessToList(p1PrevGuesses, p1CurrentGuess);
                            message = P2ThreadHandler.obtainMessage(GAME_STATUS_P1_PLAYED);
                            message.arg1 = Integer.valueOf(p1CurrentGuess);
                            P2ThreadHandler.sendMessage(message);

                            break;
                        }

                        // When P2 has played, P1 has to respond back with hints
                        case GAME_STATUS_P2_PLAYED: {
                            String response = matchGuessWithOriginal(String.valueOf(msg.arg1), p1chosenNumber);
                            message = P2ThreadHandler.obtainMessage(GAME_STATUS_P1_RESPONDED);
                            message.obj = response;
                            P2ThreadHandler.sendMessage(message);

                            break;
                        }

                        // When P2 has responded with hints, stores the hints and inform UI thread
                        case GAME_STATUS_P2_RESPONDED: {
                            String response = (String) msg.obj;
                            p1RightPosMatches = Character.getNumericValue(response.charAt(0));
                            p1WrongPosMatches = Character.getNumericValue(response.charAt(1));
                            if(response.length() == 3) {
                                p1HintNumber = Character.getNumericValue(response.charAt(2));
                            } else {
                                p1HintNumber = null;
                            }
                            // Increment the games
                            incrementGames();
                            message = UIThreadHandler.obtainMessage(P1_UPDATE_UI);
                            message.obj = response;
                            Bundle b = new Bundle();
                            b.putString("currentGuess", p1CurrentGuess);
                            message.setData(b);
                            // Inform UI thread of the guess and hints
                            UIThreadHandler.sendMessage(message);

                            break;
                        }
                    }
                }
            };

            Looper.loop();

        }
    }

    // Player 2 Thread Runnable
    public class Player2Runnable implements Runnable {
        @Override
        public void run() {
            Looper.prepare();

            P2Looper = Looper.myLooper();

            // P2 Thread Handler definition
            P2ThreadHandler = new Handler(P2Looper) {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void handleMessage(@NonNull Message msg) {

                    Message message = new Message();

                    switch(msg.what) {
                        // Intialize P2 and chose a number
                        case GAME_CMD_INIT: {
                            p2chosenNumber = getRandomNumber();
                            // inform the UI Thread when ready
                            message.what = GAME_STATUS_P2_INITIATED;
                            UIThreadHandler.sendMessage(message);

                            break;
                        }

                        // Starts to guess
                        case GAME_CMD_PLAY: {
                            // Guessing the new number w.r.t previous response from the opponent
                            p2CurrentGuess = guessNumberForP2();
                            addGuessToList(p2PrevGuesses, p2CurrentGuess);
                            message = P1ThreadHandler.obtainMessage(GAME_STATUS_P2_PLAYED);
                            message.arg1 = Integer.valueOf(p2CurrentGuess);
                            P1ThreadHandler.sendMessage(message);

                            break;
                        }

                        // When P1 has played, P2 has to respond back with hints
                        case GAME_STATUS_P1_PLAYED: {
                            String response = matchGuessWithOriginal(String.valueOf(msg.arg1), p2chosenNumber);
                            message = P1ThreadHandler.obtainMessage(GAME_STATUS_P2_RESPONDED);
                            message.obj = response;
                            P1ThreadHandler.sendMessage(message);

                            break;
                        }

                        // When P1 has responded with hints, stores the hints and inform UI thread
                        case GAME_STATUS_P1_RESPONDED: {
                            String response = (String) msg.obj;
                            p2RightPosMatches = Character.getNumericValue(response.charAt(0));
                            p2WrongPosMatches = Character.getNumericValue(response.charAt(1));
                            if(response.length() == 3) {
                                p2HintNumber = Character.getNumericValue(response.charAt(2));
                            } else {
                                p2HintNumber = null;
                            }
                            // Increment the games
                            incrementGames();
                            message = UIThreadHandler.obtainMessage(P2_UPDATE_UI);
                            message.obj = response;
                            Bundle b = new Bundle();
                            b.putString("currentGuess", p2CurrentGuess);
                            message.setData(b);
                            // inform UI of guess and hints
                            UIThreadHandler.sendMessage(message);

                            break;
                        }
                    }
                }
            };

            Looper.loop();
        }
    }

    // This method returns a complete random string of values {0 to 9} of 4 numbers, with no duplicates
    public synchronized String getRandomNumber() {
        String s = "";
        while(s.length() < gameSize) {
            String randomNumberChar = String.valueOf(new Random().nextInt(10));
            if(!s.contains(randomNumberChar)) {
                s += randomNumberChar;
            }
        }
        return s;
    }

    // Sets p1Initiated
    public synchronized void setP1Init(Boolean value) {
        p1Initiated = value;
    }

    // sets p2Initiated
    public synchronized void setP2Init(Boolean value) {
        p2Initiated = value;
    }

    // sets bothInitiated
    public synchronized void setBothInit(Boolean value) {
        bothInitiated = value;
    }

    // This method matches the guess with chosen number and returns the hints in form of a string
    // String can have either 2 or 3 elements, if there are only 2 elements that means that only correctly poitioned and incorrectly positioned number of items are sent
    // When String have a length of 3, it means that a missing digit hint also provided
    public String matchGuessWithOriginal(String guess, String chosenNumber) {
        int rightPosDigits = 0;
        int wrongPosDigits = 0;
        String missingDigitHint = null;
        String nonMatching = "";
        for(int i = 0; i < chosenNumber.length(); i++) {
            String guessChar = String.valueOf(chosenNumber.charAt(i));
            if( guess.contains(guessChar) ) {
                if(guess.indexOf(guessChar) == i) {
                    rightPosDigits += 1;
                } else {
                    wrongPosDigits += 1;
                }
            } else {
                nonMatching += guessChar;
            }
        }

        // generate digit which was not present in guess and is present in chosen number
        if(rightPosDigits + wrongPosDigits < gameSize) {
            int randomIndex = new Random().nextInt(nonMatching.length());
            missingDigitHint = Character.toString(nonMatching.charAt(randomIndex));
        }
        // Create a string
        String strToReturn = String.valueOf(rightPosDigits) + String.valueOf(wrongPosDigits);
        if(missingDigitHint != null) {
            strToReturn += String.valueOf(missingDigitHint);
        }
        return strToReturn;
    }


    // This method is the definition of guessing strategy for Player 1
    // It takes into accounts all the missing digit hints that are received from Player 2 and also consider what was the previous response
    @RequiresApi(api = Build.VERSION_CODES.N)
    public String guessNumberForP1() {
        String[] currentGuess = new String[4];
        String previousGuess = p1CurrentGuess;
        String correctSetExistsInPrevGuess = "";
        String randomIndexSequence = "";

        // check if there was any hint previously
        if(p1RightPosMatches != null && p1WrongPosMatches != null) {

            // store the missing digits if number of missing digits are less than game size i.e. 4
            if(p1CorrectGuessedNumbers.size() < gameSize) {
                if(p1HintNumber != null && !p1CorrectGuessedNumbers.stream().anyMatch(String.valueOf(p1HintNumber)::equals)) {
                    // this new missing number hint can be added to correctGuessedNumbers
                    p1CorrectGuessedNumbers.add(String.valueOf(p1HintNumber));
                }

            }

            // generate a random sequence for indexes
            while(randomIndexSequence.length() < gameSize) {
                String randomIndex = String.valueOf(new Random().nextInt(gameSize));
                if( !randomIndexSequence.contains(randomIndex) ) {
                    randomIndexSequence += randomIndex;
                }
            }

            // Check if number of missing digits are less than game size i.e. 4
            if(p1CorrectGuessedNumbers.size() < gameSize) {
                for(int i = 0; i < previousGuess.length(); i++) {
                    String currentNumber = String.valueOf(previousGuess.charAt(i));
                    if(!p1CorrectGuessedNumbers.stream().anyMatch(currentNumber::equals)) {
                        correctSetExistsInPrevGuess += currentNumber;
                    }
                }

                // first add unknown numbers matched from previous guess
                if(p1CorrectGuessedNumbers.size() > 0) {
                    for(int i = 0; i< p1CorrectGuessedNumbers.size(); i++) {
                        currentGuess[ Character.getNumericValue(randomIndexSequence.charAt(i)) ] = p1CorrectGuessedNumbers.get(i);
                    }
                }

                // Then add the numbers from missing digits list
                if(correctSetExistsInPrevGuess.length() > 0) {
                    for(int i = p1CorrectGuessedNumbers.size(); i < gameSize; i++) {
                        currentGuess[ Character.getNumericValue(randomIndexSequence.charAt(i)) ] = String.valueOf(correctSetExistsInPrevGuess.charAt(i - p1CorrectGuessedNumbers.size()));
                    }
                }

            // if missing digits list has 4 elements, then only consider this list
            } else if(p1CorrectGuessedNumbers.size() == gameSize) {
                for(int i = 0; i < p1CorrectGuessedNumbers.size(); i++) {
                    currentGuess[Character.getNumericValue(randomIndexSequence.charAt(i))] = p1CorrectGuessedNumbers.get(i);
                }
            }

        } else {
            // When there are no hints (first time)
            for(int i = 0; i < gameSize; i++) {
                while(true) {
                    String randomDigit = String.valueOf(new Random().nextInt(10));
                    if( !checkElementInArray(currentGuess, randomDigit) ) {
                        currentGuess[i] = randomDigit;
                        break;
                    }
                }
            }

        }
        // generate a new guess
        String currentGuessStr = "";
        for(int i = 0; i < currentGuess.length; i++) {
            currentGuessStr += currentGuess[i];
        }

        return currentGuessStr;

    }

    // This method checks if element is in a string array
    public Boolean checkElementInArray(String[] arr, String elm) {
        Boolean check = false;
        for(String item: arr) {
            if(item == elm) {
                check = true;
            }
        }
        return check;
    }


    // This method is the definition of guessing strategy for Player 2
    // It takes into accounts all the missing digit hints that are received from Player 1 only
    @RequiresApi(api = Build.VERSION_CODES.N)
    public String guessNumberForP2() {
        String[] currentGuess = new String[gameSize];
        String randomIndexSequence = "";
        int counter = 0;

        // generate random indexes sequence
        while(randomIndexSequence.length() < gameSize) {
            String randomIndex = String.valueOf(new Random().nextInt(gameSize));
            if( !randomIndexSequence.contains(randomIndex) ) {
                randomIndexSequence += randomIndex;
            }
        }

        // check if nothing was there is previous hints
        if(p2RightPosMatches != null && p2WrongPosMatches != null) {
            // adding missing hints to a list
            if (p2CorrectGuessedNumbers.size() < gameSize) {
                if (p2HintNumber != null && !p2CorrectGuessedNumbers.stream().anyMatch(String.valueOf(p2HintNumber)::equals)) {
                    // this new missing number hint can be added to correctGuessedNumbers
                    p2CorrectGuessedNumbers.add(String.valueOf(p2HintNumber));
                }
            }
        }
        // add missing digits first
        if(p2CorrectGuessedNumbers.size() > 0) {
            for(int i = 0; i < p2CorrectGuessedNumbers.size(); i++) {
                currentGuess[Character.getNumericValue(randomIndexSequence.charAt(i))] = p2CorrectGuessedNumbers.get(i);
                counter++;
            }
        }
        // add random number which does not match missing digits list items
        if(counter < gameSize) {
            while(counter < gameSize) {
                String randomNumber = String.valueOf(new Random().nextInt(10));
                if(!p2CorrectGuessedNumbers.stream().anyMatch(randomNumber::equals) && !checkElementInArray(currentGuess, randomNumber)) {
                    currentGuess[Character.getNumericValue(randomIndexSequence.charAt(counter))] = randomNumber;
                    counter++;
                }
            }
        }
        // generate new guess
        String currentGuessStr = "";
        for(int i = 0; i < currentGuess.length; i++) {
            currentGuessStr += currentGuess[i];
        }

        return currentGuessStr;

    }

    // This method adds the guess to particular list for storage
    public synchronized void addGuessToList(ArrayList<String> list, String guess) {
        list.add(guess);
    }

    // This method returns the a String which includes all the previous guesses for P1
    public synchronized String getP1GuessesList() {
        String toReturn = "    ";
        for(int i = 0; i < p1PrevGuesses.size(); i++) {
            toReturn += p1PrevGuesses.get(i);
            toReturn += "    ";
        }
        return toReturn;
    }

    // This method returns the a String which includes all the previous guesses for P2
    public synchronized String getP2GuessesList() {
        String toReturn = "    ";
        for(int i = 0; i < p2PrevGuesses.size(); i++) {
            toReturn += p2PrevGuesses.get(i);
            toReturn += "    ";
        }
        return toReturn;
    }

    // Increment games
    public synchronized void incrementGames() {
        numberOfGamesPlayed += 1;
    }

    // get games
    public synchronized int getGamesPlayed() {
        return numberOfGamesPlayed;
    }

    // reset games
    public synchronized void resetGames() {
        numberOfGamesPlayed = 0;
        setP1Init(false);
        setP2Init(false);
        setBothInit(false);
    }

    // Clear view fields
    public void clearTextFields() {
        p1Container.setBackgroundResource(R.color.white);
        p2Container.setBackgroundResource(R.color.white);
        p1GuessField.setText("----");
        p2GuessField.setText("----");
        p1NumberField.setText("----");
        p2NumberField.setText("----");
        p1Hint1Field.setText("-");
        p2Hint1Field.setText("-");
        p1Hint2Field.setText("-");
        p2Hint2Field.setText("-");
        p1HintMissingField.setText("-");
        p2HintMissingField.setText("-");
    }

    // reset data variables
    public void resetData() {
        p1chosenNumber = "";
        p2chosenNumber = "";
        p1CurrentGuess = "";
        p2CurrentGuess = "";
        p1RightPosMatches = null;
        p1WrongPosMatches = null;
        p1HintNumber = null;

        p2RightPosMatches = null;
        p2WrongPosMatches = null;
        p2HintNumber = null;

        p1CorrectGuessedNumbers = new ArrayList<String>();
        p2CorrectGuessedNumbers = new ArrayList<String>();

        p1PrevGuesses = new ArrayList<String>();
        p2PrevGuesses = new ArrayList<String>();
    }

    // stopping the threads by removing all messages from all handlers
    public void resetThreads() {
        UIThreadHandler.removeMessages(GAME_CMD_START);
        UIThreadHandler.removeMessages(GAME_STATUS_P1_INITIATED);
        UIThreadHandler.removeMessages(GAME_STATUS_P2_INITIATED);
        UIThreadHandler.removeMessages(P1_UPDATE_UI);
        UIThreadHandler.removeMessages(P2_UPDATE_UI);
        UIThreadHandler.removeMessages(GAME_STATUS_P1_WON);
        UIThreadHandler.removeMessages(GAME_STATUS_P2_WON);
        UIThreadHandler.removeMessages(HALT_PLAY);

        P1ThreadHandler.removeMessages(GAME_CMD_INIT);
        P1ThreadHandler.removeMessages(GAME_CMD_PLAY);
        P1ThreadHandler.removeMessages(GAME_STATUS_P2_PLAYED);
        P1ThreadHandler.removeMessages(GAME_STATUS_P2_RESPONDED);

        P2ThreadHandler.removeMessages(GAME_CMD_INIT);
        P2ThreadHandler.removeMessages(GAME_CMD_PLAY);
        P2ThreadHandler.removeMessages(GAME_STATUS_P1_PLAYED);
        P2ThreadHandler.removeMessages(GAME_STATUS_P1_RESPONDED);

    }

}