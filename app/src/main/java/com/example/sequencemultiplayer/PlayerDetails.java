package com.example.sequencemultiplayer;

public class PlayerDetails {

    private String playerID;
    private String playerName;
    private String imgProfilePicURL;


    public PlayerDetails(String playerID, String playerName, String imgProfilePicURL) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.imgProfilePicURL = imgProfilePicURL;
    }

    public String getPlayerID() {
        return playerID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getImgProfilePicURL() {
        return imgProfilePicURL;
    }
}
