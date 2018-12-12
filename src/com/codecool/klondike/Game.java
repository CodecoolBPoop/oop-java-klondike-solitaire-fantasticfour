package com.codecool.klondike;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.print.Collation;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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




    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if(e.getClickCount() == 2){
            moveCardToFoundation(card);
            return;
        }
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        if(e.getClickCount() == 2) {
            return;
        }
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK || activePile.getPileType() == Pile.PileType.FOUNDATION)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards = null;
        }
    };

    public boolean isGameWon() {
        //TODO
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
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
        if(destPile.getCards().size() != 0){
            if (card.getRank() == (destPile.getTopCard().getRank()-1) ){
                if (Card.isOppositeColor(card, destPile.getTopCard())){
                    isValidMove = true;
                }
            }
        } else if (card.getRank() == 13){
            isValidMove = true;
        }
        if (isValidMove) {
            flipCard(card);
            }
        return isValidMove;
    }

    public void flipCard(Card card) {
        int previousCard = card.getContainingPile().getCards().size() - 2;
        if (!card.getContainingPile().isEmpty() && !card.getContainingPile().getPileType().equals(Pile.PileType.DISCARD) && previousCard > -1) {
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

    public void dealCards() {
        //Iterator<Card> deckIterator = deck.iterator();
        //TODO
        Collections.shuffle(deck);
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
}
