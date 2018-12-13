package com.codecool.klondike;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.print.Collation;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.control.Button;

import java.util.*;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    public int validMoveCounter = 0;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        if (e.getButton().toString().equals("PRIMARY")){
            Card card = (Card) e.getSource();
            if(e.getClickCount() == 2){
                moveCardToFoundation(card);
                return;
            }
            if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
                if (card != card.getContainingPile().getTopCard()) return;
                card.moveToPile(discardPile);
                card.flip();
                card.setMouseTransparent(false);
                System.out.println("Placed " + card + " to the waste.");
            }
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        if (e.getButton().toString().equals("PRIMARY")) {
            refillStockFromDiscard();
        }
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        if (e.getButton().toString().equals("PRIMARY")){
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
        }
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        if (e.getButton().toString().equals("PRIMARY")){
            if(e.getClickCount() == 2) {
                return;
            }
            Card card = (Card) e.getSource();
            Pile activePile = card.getContainingPile();
            if (activePile.getPileType() == Pile.PileType.STOCK || card.isFaceDown() || (card != activePile.getTopCard() && activePile.getPileType() == Pile.PileType.DISCARD))
                return;
            double offsetX = e.getSceneX() - dragStartX;
            double offsetY = e.getSceneY() - dragStartY;

            draggedCards.clear();
            int idx = activePile.getCards().size();
            for (int i = 0; i < activePile.getCards().size(); i++) {
                if (card.equals(activePile.getCards().get(i))) {
                    idx = i;
                }
            }

            for (int i = idx; i < activePile.getCards().size(); i++) {
                if(!activePile.getPileType().equals(Pile.PileType.DISCARD)){
                    if(!activePile.getCards().get(i).isFaceDown()){
                        draggedCards.add(activePile.getCards().get(i));
                    }
                } else{
                    draggedCards.add(card);
                }
            }
            for (Card grabbedCard:draggedCards){
                grabbedCard.getDropShadow().setRadius(20);
                grabbedCard.getDropShadow().setOffsetX(10);
                grabbedCard.getDropShadow().setOffsetY(10);

                grabbedCard.toFront();
                grabbedCard.setTranslateX(offsetX);
                grabbedCard.setTranslateY(offsetY);
            }
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (e.getButton().toString().equals("PRIMARY")) {
            if (draggedCards.isEmpty())
                return;
            Card card = (Card) e.getSource();
            Pile pile = getValidIntersectingPile(card, tableauPiles);
            if (pile != null && !pile.equals(card.getContainingPile())) {
                handleValidMove(card, pile);
            } else {
                draggedCards.forEach(MouseUtil::slideBack);
                draggedCards.clear();
            }
        }
    };

    public boolean isGameWon() {
        boolean gameWon = true;
        for (int i = 0; i < foundationPiles.size(); i++) {
                if (foundationPiles.get(i).getCards().size() != 13){
                    gameWon = false;
                }
        }
        return gameWon;
    }


    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        shuffleCards();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        //TODO
        List<Card> cards = new ArrayList<>(discardPile.getCards());
        Collections.reverse(cards);
        for (Card card: cards) {
            card.flip();
            card.moveToPile(stockPile);
        }
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        boolean isValidMove = false;
        int previousCard = card.getContainingPile().getCards().size()-2;
        if(destPile.getCards().size() != 0){
            if (card.getRank() == (destPile.getTopCard().getRank()-1) ){
                if (Card.isOppositeColor(card, destPile.getTopCard())){
                    isValidMove = true;
                    validMoveCounter += 1;
                }
            }
        } else if (card.getRank() == 13){
            isValidMove = true;
            validMoveCounter += 1;
        }
        if (isValidMove) {
            if(card.getContainingPile().getCards().size() != draggedCards.size() && !card.getContainingPile().getPileType().equals(Pile.PileType.DISCARD) && previousCard > -1){
                if(draggedCards.size()<2){
                    Card cardToFlip = card.getContainingPile().getCards().get(previousCard);
                    if(cardToFlip.isFaceDown()){
                        cardToFlip.flip();
                    }
                } else{
                    Card cardToFlip = card.getContainingPile().getCards().get(card.getContainingPile().getCards().size()-(draggedCards.size()+1));
                    if(cardToFlip.isFaceDown()){
                        cardToFlip.flip();
                    }
                }
            }
        }
        return isValidMove;
    }

    public void flipCard(Card card) {
        int previousCard = card.getContainingPile().getCards().size() - 2;
        if (!card.getContainingPile().isEmpty() && !card.getContainingPile().getPileType().equals(Pile.PileType.DISCARD) && previousCard > -1 && card.getContainingPile().getCards().get(previousCard).isFaceDown()) {
            card.getContainingPile().getCards().get(previousCard).flip();
        }
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = card.getContainingPile();
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void shuffleCards() {
        Collections.shuffle(deck);
    }

    public void dealCards() {

        //Iterator<Card> deckIterator = deck.iterator();
        //TODO
        for (int i = 0; i < deck.size(); i++) {
            Card card = deck.get(i);
            if(i == 0){
                tableauPiles.get(0).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                card.flip();
            } else if(i<3){
                tableauPiles.get(1).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==2){
                    card.flip();
                }
            } else if(i<6){
                tableauPiles.get(2).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==5){
                    card.flip();
                }
            } else if(i<10){
                tableauPiles.get(3).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==9){
                    card.flip();
                }
            } else if(i<15){
                tableauPiles.get(4).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==14){
                    card.flip();
                }
            } else if(i<21){
                tableauPiles.get(5).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==20){
                    card.flip();
                }
            } else if(i<28){
                tableauPiles.get(6).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                if(i==27){
                    card.flip();
                }
            } else {
                stockPile.addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
            }
        }
        /* ASK THE MENTORS TOMORROW
         * int iterationNumber = 0;
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
            iterationNumber++;
        });
         */

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void moveCardToFoundation(Card card) {
        int rank = card.getRank();
        int suit = card.getSuit();
        // finishGame(); // FOR TESTING - ACE COMPLETES THE GAME
        int topCardRank;
        try {
            topCardRank = foundationPiles.get(suit-1).getTopCard().getRank();
        } catch (NullPointerException e) {
            topCardRank = 0;
        }
        if (topCardRank+1 == rank && !card.isFaceDown()) {
            flipCard(card);
            card.moveToPile(foundationPiles.get(suit-1));
        }
        if (isGameWon()) showWinAlert();
    }

    public void addRestartButton() {
        Button restartButton = new Button("restart");
        HBox buttonBar = new HBox();
        restartButton.setOnAction(actionEvent -> restartGame());
        buttonBar.getChildren().add(restartButton);
        getChildren().add(buttonBar);

    }

    private void restartGame() {
        for (Card card: deck) {
            getChildren().remove(card);
        }
        resetPiles();
        deck.clear();
        deck = Card.createNewDeck();
        shuffleCards();
        dealCards();
    }

    private void resetPiles () {
        for (Pile pile: tableauPiles) {
            pile.clear();
        }
        for (Pile pile: foundationPiles) {
            pile.clear();
        }
        discardPile.clear();
        stockPile.clear();
    }

    private void finishGame() {
        for (Card card: deck) {
            getChildren().remove(card);
        }
        resetPiles();
        deck.clear();
        deck = Card.createNewDeck();
        dealCards();
        int counter = 0;
        for (Card card: deck) {
            card.flip();
            if (counter < 13) card.moveToPile(foundationPiles.get(0));
            else if (counter < 26) card.moveToPile(foundationPiles.get(1));
            else if (counter < 39) card.moveToPile(foundationPiles.get(2));
            else card.moveToPile(foundationPiles.get(3));
            counter++;
        }
    }

    private void showWinAlert () {
        Alert winAlert = new Alert(Alert.AlertType.INFORMATION);
        ButtonType buttonRestart = new ButtonType("Restart");
        ButtonType buttonQuit = new ButtonType("Quit");

        winAlert.getButtonTypes().setAll(buttonRestart, buttonQuit);

        winAlert.setTitle("YOU WIN!");
        winAlert.setHeaderText("WON");
        winAlert.setContentText("It took " + validMoveCounter +" steps to solve the game!" + " \nPress OK to restart!");

        Optional<ButtonType> result = winAlert.showAndWait();

        if (result.isPresent() && result.get() == buttonRestart) restartGame();
        if (result.isPresent() && result.get() == buttonQuit) Platform.exit();


    }
}
