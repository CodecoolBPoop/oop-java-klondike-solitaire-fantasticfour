package com.codecool.klondike;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.*;

public class Card extends ImageView {

    private int suit;
    private int rank;
    private boolean faceDown;

    private Image backFace;
    private Image frontFace;
    private Pile containingPile;
    private DropShadow dropShadow;

    static Image cardBackImage;
    private static final Map<String, Image> cardFaceImages = new HashMap<>();
    public static final int WIDTH = 150;
    public static final int HEIGHT = 215;

    public Card(int suit, int rank, boolean faceDown) {
        this.suit = suit;
        this.rank = rank;
        this.faceDown = faceDown;
        this.dropShadow = new DropShadow(2, Color.gray(0, 0.75));
        backFace = cardBackImage;
        frontFace = cardFaceImages.get(getShortName());
        setImage(faceDown ? backFace : frontFace);
        setEffect(dropShadow);
    }

    public int getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    public boolean isFaceDown() {
        return faceDown;
    }

    public String getShortName() {
        return "S" + suit + "R" + rank;
    }

    public DropShadow getDropShadow() {
        return dropShadow;
    }

    public Pile getContainingPile() {
        return containingPile;
    }

    public void setContainingPile(Pile containingPile) {
        this.containingPile = containingPile;
    }

    public void moveToPile(Pile destPile) {
        this.getContainingPile().getCards().remove(this);
        destPile.addCard(this);
    }

    public void flip() {
        faceDown = !faceDown;
        setImage(faceDown ? backFace : frontFace);
    }

    @Override
    public String toString() {
        return "The " + "Rank" + rank + " of " + "Suit" + suit;
    }

    public static boolean isOppositeColor(Card card1, Card card2) {
        boolean oppositeColor = false;
        Integer[] red = {1, 2};
        Integer[] black = {3, 4};
        Integer card1Color = 0;
        Integer card2Color = 0;

        card1Color = card1.getSuit();
        card2Color = card2.getSuit();

        if (Arrays.asList(red).contains(card1Color) && Arrays.asList(black).contains(card2Color)){
            oppositeColor = true;
    }
        if(Arrays.asList(red).contains(card2Color) && Arrays.asList(black).contains(card1Color)) {
            oppositeColor = true;
        }
        return oppositeColor;
    }

    public static boolean isSameSuit(Card card1, Card card2) {
        return card1.getSuit() == card2.getSuit();
    }

    public static List<Card> createNewDeck() {
        List<Card> result = new ArrayList<>();
        for (int suit = 1; suit < 5; suit++) {
            for (int rank = 1; rank < 14; rank++) {
                result.add(new Card(suit, rank, true));
            }
        }
        return result;
    }

    public static boolean rankCheck(Card card1,Card card2){return card1.getRank()-1==card2.getRank();}

    public static void loadCardImages() {
        cardBackImage = new Image("card_images/card_back.png");
        String suitName = "";
        for (int suit = 1; suit < 5; suit++) {
            switch (suit) {
                case 1:
                    suitName = "hearts";
                    break;
                case 2:
                    suitName = "diamonds";
                    break;
                case 3:
                    suitName = "spades";
                    break;
                case 4:
                    suitName = "clubs";
                    break;
            }
            for (int rank = 1; rank < 14; rank++) {
                String cardName = suitName + rank;
                String cardId = "S" + suit + "R" + rank;
                String imageFileName = "card_images/" + cardName + ".png";
                cardFaceImages.put(cardId, new Image(imageFileName));
            }
        }
    }

}
