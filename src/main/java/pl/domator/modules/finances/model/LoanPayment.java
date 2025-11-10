package pl.domator.modules.finances.model;

import java.time.LocalDate;

public class LoanPayment {
    private int id;
    private int loanId;
    private LocalDate paymentDate;
    private double amount;
    private String paymentType; // "rata" lub "nadpłata"
    private String note;

    public LoanPayment() {}

    public LoanPayment(int id, int loanId, LocalDate paymentDate, double amount,
                       String paymentType, String note) {
        this.id = id;
        this.loanId = loanId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentType = paymentType;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    @Override
    public String toString() {
        return paymentDate + " - " + amount + " zł (" + paymentType + ")";
    }
}
