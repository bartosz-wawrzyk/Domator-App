package pl.domator.Modules.Finances.Model;

import java.time.LocalDate;

public class Loan {
    private int id;
    private int userId;
    private String bankName;
    private String loanSubject;
    private double totalAmount;
    private double paidAmount;
    private double remainingAmount;
    private double monthlyPayment;
    private double overpaymentSum;
    private LocalDate dueDate;
    private boolean overpaymentSameAccount;

    public Loan() {}

    public Loan(int id, int userId, String bankName, String loanSubject,
                double totalAmount, double paidAmount, double remainingAmount,
                double monthlyPayment, double overpaymentSum,
                LocalDate dueDate, boolean overpaymentSameAccount) {
        this.id = id;
        this.userId = userId;
        this.bankName = bankName;
        this.loanSubject = loanSubject;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.remainingAmount = remainingAmount;
        this.monthlyPayment = monthlyPayment;
        this.overpaymentSum = overpaymentSum;
        this.dueDate = dueDate;
        this.overpaymentSameAccount = overpaymentSameAccount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getLoanSubject() { return loanSubject; }
    public void setLoanSubject(String loanSubject) { this.loanSubject = loanSubject; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(double remainingAmount) { this.remainingAmount = remainingAmount; }

    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public double getOverpaymentSum() { return overpaymentSum; }
    public void setOverpaymentSum(double overpaymentSum) { this.overpaymentSum = overpaymentSum; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isOverpaymentSameAccount() { return overpaymentSameAccount; }
    public void setOverpaymentSameAccount(boolean overpaymentSameAccount) { this.overpaymentSameAccount = overpaymentSameAccount; }

    @Override
    public String toString() {
        return bankName + " - " + loanSubject + " (" + totalAmount + " zł)";
    }
}
