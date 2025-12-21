package com.mihai.overview.entity;

import jakarta.persistence.*;

import java.sql.Time;
import java.util.Date;

@Table(name = "Reviews")
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String reviewedName;

    @Column(nullable = false)
    private long ticketID;

    @Column(nullable = false)
    private Date interactionDate;

    @Column(nullable = false)
    private Time interactionTime;

    @Column(nullable = false)
    private long cid;

    @Column(nullable = false)
    private int finalGrade;

    @Column(nullable = false)
    private boolean accepted;

    //private User reviewerName

    //Default constructor (required by JPA)

    public Review() {}

    public Review(String type, String reviewedName, long ticketID, Date interactionDate, Time interactionTime, long cid, int finalGrade, boolean accepted) {
        this.type = type;
        this.reviewedName = reviewedName;
        this.ticketID = ticketID;
        this.interactionDate = interactionDate;
        this.interactionTime = interactionTime;
        this.cid = cid;
        this.finalGrade = finalGrade;
        this.accepted = accepted;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReviewedName() {
        return reviewedName;
    }

    public void setReviewedName(String reviewedName) {
        this.reviewedName = reviewedName;
    }

    public long getTicketID() {
        return ticketID;
    }

    public void setTicketID(long ticketID) {
        this.ticketID = ticketID;
    }

    public Date getInteractionDate() {
        return interactionDate;
    }

    public void setInteractionDate(Date interactionDate) {
        this.interactionDate = interactionDate;
    }

    public Time getInteractionTime() {
        return interactionTime;
    }

    public void setInteractionTime(Time interactionTime) {
        this.interactionTime = interactionTime;
    }

    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public int getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(int finalGrade) {
        this.finalGrade = finalGrade;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
