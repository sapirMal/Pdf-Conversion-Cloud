public class userHandler {
    private int activeMsgs;
    private String acc;
    private String id;
    private String qurl;

    public userHandler(String id, int activeMsgs, String acc, String qurl) {
        this.id = id;
        this.activeMsgs = activeMsgs;
        this.acc = acc;
        this.qurl = qurl;
    }

    public String getQurl() {
        return qurl;
    }

    public void setQurl(String qurl) {
        this.qurl = qurl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getActiveMsgs() {
        return activeMsgs;
    }

    public void setActiveMsgs(int activeMsgs) {
        this.activeMsgs = activeMsgs;
    }

    public String getAcc() {
        return acc;
    }

    public void setAcc(String acc) {
        this.acc = acc;
    }
}