package pl.domator.Modules.Finances.Controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pl.domator.Config.DBConnectionController;
import pl.domator.Modules.Finances.Model.Loan;
import pl.domator.Modules.Finances.Model.LoanPayment;
import pl.domator.Core.LoggerUtils;
import pl.domator.Modules.UI.DashboardController;

import java.sql.*;
import java.util.Optional;

public class CreditsController implements DashboardController.UserAware {

    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, String> bankColumn, subjectColumn;
    @FXML private TableColumn<Loan, Double> totalColumn, remainingColumn;
    @FXML private TextField bankField, subjectField, totalField, installmentField, paidField, overpaymentField, remainingField;
    @FXML private DatePicker dueDatePicker;
    @FXML private CheckBox sameAccountCheckBox;
    @FXML private Button addLoanBtn, editLoanBtn, deleteLoanBtn, saveLoanBtn, cancelLoanBtn;

    @FXML private TableView<LoanPayment> paymentsTable;
    @FXML private TableColumn<LoanPayment, Date> dateColumn;
    @FXML private TableColumn<LoanPayment, Double> amountColumn;
    @FXML private TableColumn<LoanPayment, String> typeColumn, noteColumn;
    @FXML private DatePicker paymentDatePicker;
    @FXML private TextField paymentAmountField, paymentNoteField;
    @FXML private ComboBox<String> paymentTypeCombo;
    @FXML private Button addPaymentBtn, editPaymentBtn, deletePaymentBtn, savePaymentBtn, cancelPaymentBtn;

    private ObservableList<Loan> loans = FXCollections.observableArrayList();
    private ObservableList<LoanPayment> payments = FXCollections.observableArrayList();
    private int userId;

    private boolean isAddingLoan = false;
    private boolean isEditingLoan = false;
    private boolean isAddingPayment = false;
    private boolean isEditingPayment = false;

    @FXML
    private void initialize() {
        paymentTypeCombo.setItems(FXCollections.observableArrayList("rata", "nadpłata"));
        setupTables();
        setupBindings();
        setLoanFormDisabled(true);
        setPaymentFormDisabled(true);
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
        loadLoans();
    }

    private void setupTables() {
        bankColumn.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getBankName));
        subjectColumn.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getLoanSubject));
        totalColumn.setCellValueFactory(d -> Bindings.createObjectBinding(d.getValue()::getTotalAmount));
        remainingColumn.setCellValueFactory(d -> Bindings.createObjectBinding(d.getValue()::getRemainingAmount));

        dateColumn.setCellValueFactory(d -> Bindings.createObjectBinding(() -> Date.valueOf(d.getValue().getPaymentDate())));
        amountColumn.setCellValueFactory(d -> Bindings.createObjectBinding(d.getValue()::getAmount));
        typeColumn.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getPaymentType));
        noteColumn.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getNote));
    }

    private void setupBindings() {
        loansTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            try {
                if (newSel != null) {
                    fillLoanFields(newSel);
                    loadPayments(newSel.getId());

                    editLoanBtn.setDisable(false);
                    deleteLoanBtn.setDisable(false);
                    addPaymentBtn.setDisable(false);
                } else {
                    clearLoanFields();
                    payments.clear();
                    editLoanBtn.setDisable(true);
                    deleteLoanBtn.setDisable(true);
                    addPaymentBtn.setDisable(true);
                }
            } catch (Exception ex) {
                LoggerUtils.logError(ex);
            }
        });
        paymentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                fillPaymentFields(newSel);
                editPaymentBtn.setDisable(false);
                deletePaymentBtn.setDisable(false);
            } else {
                clearPaymentFields();
                editPaymentBtn.setDisable(true);
                deletePaymentBtn.setDisable(true);
            }
        });
    }

    private void fillPaymentFields(LoanPayment p) {
        paymentDatePicker.setValue(p.getPaymentDate());
        paymentAmountField.setText(String.valueOf(p.getAmount()));
        paymentTypeCombo.setValue(p.getPaymentType());
        paymentNoteField.setText(p.getNote());
    }

    private void setLoanFormDisabled(boolean disable) {
        bankField.setDisable(disable);
        subjectField.setDisable(disable);
        totalField.setDisable(disable);
        dueDatePicker.setDisable(disable);
        sameAccountCheckBox.setDisable(disable);

        installmentField.setEditable(false);
        paidField.setEditable(false);
        overpaymentField.setEditable(false);
        remainingField.setEditable(false);
        highlightActiveFields(!disable, bankField, subjectField, totalField, dueDatePicker);
    }

    private void setPaymentFormDisabled(boolean disable) {
        paymentDatePicker.setDisable(disable);
        paymentAmountField.setDisable(disable);
        paymentTypeCombo.setDisable(disable);
        paymentNoteField.setDisable(disable);
        highlightActiveFields(!disable,
                paymentDatePicker, paymentAmountField, paymentTypeCombo, paymentNoteField);
    }

    private void highlightActiveFields(boolean active, Control... controls) {
        String activeStyle = "-fx-border-color: #4287f5; -fx-border-width: 1.5; -fx-background-color: #f9fcff;";
        String inactiveStyle = "";
        for (Control c : controls) {
            c.setStyle(active ? activeStyle : inactiveStyle);
        }
    }

    private void toggleLoanButtons(boolean editing) {
        addLoanBtn.setDisable(editing);
        editLoanBtn.setDisable(editing);
        deleteLoanBtn.setDisable(editing);
        saveLoanBtn.setDisable(!editing);
        cancelLoanBtn.setDisable(!editing);
    }

    private void togglePaymentButtons(boolean editing) {
        addPaymentBtn.setDisable(editing);
        editPaymentBtn.setDisable(editing);
        deletePaymentBtn.setDisable(editing);
        savePaymentBtn.setDisable(!editing);
        cancelPaymentBtn.setDisable(!editing);
    }

    private boolean validateLoanInputs() {
        if (bankField.getText().isEmpty() || subjectField.getText().isEmpty() ||
                totalField.getText().isEmpty() || dueDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Uzupełnij wymagane pola: Bank, Przedmiot, Kwota całkowita, Termin.");
            return false;
        }
        try { Double.parseDouble(totalField.getText()); }
        catch (NumberFormatException e) { showAlert(Alert.AlertType.WARNING, "Kwota całkowita musi być liczbą."); return false; }
        return true;
    }

    private boolean validatePaymentInputs() {
        if (paymentDatePicker.getValue() == null || paymentAmountField.getText().isEmpty() || paymentTypeCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Uzupełnij datę, kwotę i typ wpłaty.");
            return false;
        }
        try { Double.parseDouble(paymentAmountField.getText()); }
        catch (NumberFormatException e) { showAlert(Alert.AlertType.WARNING, "Kwota musi być liczbą."); return false; }
        return true;
    }

    private boolean confirmAction(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Potwierdzenie");
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void loadLoans() {
        if (userId <= 0) return;
        loans.clear();
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.loans WHERE user_id=? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("bank_name"), rs.getString("loan_subject"),
                        rs.getDouble("total_amount"), rs.getDouble("paid_amount"),
                        rs.getDouble("remaining_amount"), rs.getDouble("monthly_payment"),
                        rs.getDouble("overpayment_sum"),
                        rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                        rs.getBoolean("overpayment_same_account")
                ));
            }
            loansTable.setItems(loans);

            if (!loans.isEmpty()) {
                Loan first = loans.get(0);
                loansTable.getSelectionModel().select(first);
                loadPayments(first.getId());
            } else {
                payments.clear();
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void loadPayments(int loanId) {
        payments.clear();
        try (Connection conn = DBConnectionController.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM dmt.loan_payments WHERE loan_id=? ORDER BY payment_date DESC")) {
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                payments.add(new LoanPayment(
                        rs.getInt("id"), rs.getInt("loan_id"),
                        rs.getDate("payment_date").toLocalDate(),
                        rs.getDouble("amount"), rs.getString("payment_type"),
                        rs.getString("note")
                ));
            }
            paymentsTable.setItems(payments);
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    private void fillLoanFields(Loan l) {
        bankField.setText(l.getBankName());
        subjectField.setText(l.getLoanSubject());
        totalField.setText(String.format("%.2f", l.getTotalAmount()));
        installmentField.setText(String.format("%.2f", l.getMonthlyPayment())); // teraz to suma rat!
        paidField.setText(String.format("%.2f", l.getPaidAmount()));             // również suma rat, jeśli chcesz
        overpaymentField.setText(String.format("%.2f", l.getOverpaymentSum()));
        remainingField.setText(String.format("%.2f", l.getRemainingAmount()));
        dueDatePicker.setValue(l.getDueDate());
        sameAccountCheckBox.setSelected(l.isOverpaymentSameAccount());
    }

    private void clearLoanFields() {
        bankField.clear(); subjectField.clear(); totalField.clear(); installmentField.clear();
        paidField.clear(); overpaymentField.clear(); remainingField.clear();
        dueDatePicker.setValue(null); sameAccountCheckBox.setSelected(false);
    }

    private void clearPaymentFields() {
        paymentDatePicker.setValue(null);
        paymentAmountField.clear();
        paymentTypeCombo.getSelectionModel().clearSelection();
        paymentNoteField.clear();
    }

    @FXML private void handleAddLoan() {
        clearLoanFields();
        setLoanFormDisabled(false);
        toggleLoanButtons(true);
        isAddingLoan = true; isEditingLoan = false;
        System.out.println("id user "+ userId);
    }

    @FXML private void handleEditLoan() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Najpierw wybierz kredyt z listy.");
            return;
        }

        if (loansTable.getSelectionModel().getSelectedItem() == null) return;
        setLoanFormDisabled(false);
        toggleLoanButtons(true);
        isAddingLoan = false; isEditingLoan = true;
    }

    @FXML private void handleCancelLoan() {
        setLoanFormDisabled(true);
        toggleLoanButtons(false);
        isAddingLoan = false; isEditingLoan = false;
        clearLoanFields();
    }

    @FXML
    private void handleDeleteLoan() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Najpierw wybierz kredyt z listy.");
            return;
        }

        try (Connection conn = DBConnectionController.getConnection()) {
            if (conn == null) throw new SQLException("Brak połączenia z bazą danych.");

            PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT COUNT(*) FROM dmt.loan_payments WHERE loan_id = ?");
            checkPs.setInt(1, selected.getId());
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int paymentCount = rs.getInt(1);

            if (paymentCount > 0) {
                showAlert(Alert.AlertType.WARNING,
                        "Nie można usunąć kredytu, ponieważ posiada on powiązane wpłaty ("
                                + paymentCount + "). Usuń wpłaty przed usunięciem kredytu.");
                return;
            }

            if (!confirmAction("Czy na pewno chcesz usunąć ten kredyt?")) {
                return;
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM dmt.loans WHERE id=?");
            ps.setInt(1, selected.getId());
            ps.executeUpdate();

            loadLoans();
            payments.clear();
            clearLoanFields();

            showAlert(Alert.AlertType.INFORMATION, "Kredyt został usunięty.");
        } catch (SQLException e) {
            LoggerUtils.logError(e);
            showAlert(Alert.AlertType.ERROR, "Błąd podczas usuwania kredytu: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveLoan() {
        if (!validateLoanInputs()) return;
        if (!confirmAction("Czy chcesz zapisać zmiany?")) return;

        try (Connection conn = DBConnectionController.getConnection()) {
            if (conn == null) throw new SQLException("Brak połączenia z bazą.");

            String totalText = totalField.getText().trim().replace(',', '.');
            String installmentText = installmentField.getText().trim().replace(',', '.');
            double totalVal = Double.parseDouble(totalText);
            double installmentVal = installmentText.isEmpty() ? 0.0 : Double.parseDouble(installmentText);

            if (isAddingLoan) {
                PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO dmt.loans (user_id, bank_name, loan_subject, total_amount, monthly_payment, due_date, overpayment_same_account)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """);
                ps.setInt(1, userId);
                ps.setString(2, bankField.getText().trim());
                ps.setString(3, subjectField.getText().trim());
                ps.setDouble(4, totalVal);
                ps.setDouble(5, installmentVal);
                ps.setDate(6, Date.valueOf(dueDatePicker.getValue()));
                ps.setBoolean(7, sameAccountCheckBox.isSelected());
                ps.executeUpdate();
            } else if (isEditingLoan) {
                Loan selected = loansTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showAlert(Alert.AlertType.WARNING, "Brak wybranego kredytu do edycji.");
                    return;
                }
                PreparedStatement ps = conn.prepareStatement("""
                UPDATE dmt.loans SET bank_name=?, loan_subject=?, total_amount=?, monthly_payment=?, due_date=?, overpayment_same_account=? WHERE id=?
            """);
                ps.setString(1, bankField.getText().trim());
                ps.setString(2, subjectField.getText().trim());
                ps.setDouble(3, totalVal);
                ps.setDouble(4, installmentVal);
                ps.setDate(5, Date.valueOf(dueDatePicker.getValue()));
                ps.setBoolean(6, sameAccountCheckBox.isSelected());
                ps.setInt(7, selected.getId());
                ps.executeUpdate();
            }

            setLoanFormDisabled(true);
            toggleLoanButtons(false);
            isAddingLoan = false;
            isEditingLoan = false;

            loadLoans();

            if (!loans.isEmpty()) {
                Loan first = loans.get(0);
                loansTable.getSelectionModel().select(first);
                loadPayments(first.getId());
            }

            showAlert(Alert.AlertType.INFORMATION, "Zapisano kredyt.");
        } catch (NumberFormatException nfe) {
            showAlert(Alert.AlertType.ERROR, "Błąd parsowania liczby: " + nfe.getMessage());
            LoggerUtils.logError(nfe);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd zapisu: " + e.getMessage());
            LoggerUtils.logError(e);
        }
    }

    @FXML private void handleAddPayment() {
        clearPaymentFields();
        setPaymentFormDisabled(false);
        togglePaymentButtons(true);
        isAddingPayment = true; isEditingPayment = false;
    }

    @FXML private void handleEditPayment() {
        if (paymentsTable.getSelectionModel().getSelectedItem() == null) return;
        setPaymentFormDisabled(false);
        togglePaymentButtons(true);
        isAddingPayment = false; isEditingPayment = true;
    }

    @FXML
    private void handleCancelPayment() {
        setPaymentFormDisabled(true);
        togglePaymentButtons(false);
        isAddingPayment = false;
        isEditingPayment = false;
        clearPaymentFields();

        Loan selectedLoan = loansTable.getSelectionModel().getSelectedItem();
        if (selectedLoan != null) {
            loadPayments(selectedLoan.getId());
        }
    }

    @FXML
    private void handleDeletePayment() {
        LoanPayment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Najpierw wybierz wpłatę z listy.");
            return;
        }

        if (!confirmAction("Czy na pewno chcesz usunąć tę wpłatę?")) {
            return;
        }

        try (Connection conn = DBConnectionController.getConnection()) {
            if (conn == null) throw new SQLException("Brak połączenia z bazą danych.");

            PreparedStatement ps = conn.prepareStatement("DELETE FROM dmt.loan_payments WHERE id=?");
            ps.setInt(1, selected.getId());
            ps.executeUpdate();

            loadPayments(selected.getLoanId());
            recalculateLoanTotals(selected.getLoanId());

            showAlert(Alert.AlertType.INFORMATION, "Wpłata została usunięta.");

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            showAlert(Alert.AlertType.ERROR, "Błąd podczas usuwania wpłaty: " + e.getMessage());
        }
    }

    @FXML
    private void handleSavePayment() {
        if (!validatePaymentInputs()) return;
        if (!confirmAction("Czy chcesz zapisać tę wpłatę?")) return;

        Loan selectedLoan = loansTable.getSelectionModel().getSelectedItem();
        if (selectedLoan == null) {
            showAlert(Alert.AlertType.WARNING, "Najpierw wybierz kredyt.");
            return;
        }

        try (Connection conn = DBConnectionController.getConnection()) {
            if (conn == null) throw new SQLException("Brak połączenia z bazą.");

            String amountText = paymentAmountField.getText().trim().replace(',', '.');
            double amountVal = Double.parseDouble(amountText);

            if (isAddingPayment) {
                PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO dmt.loan_payments (loan_id, payment_date, amount, payment_type, note)
                VALUES (?, ?, ?, ?, ?)
            """);
                ps.setInt(1, selectedLoan.getId());
                ps.setDate(2, Date.valueOf(paymentDatePicker.getValue()));
                ps.setDouble(3, amountVal);
                ps.setString(4, paymentTypeCombo.getValue());
                ps.setString(5, paymentNoteField.getText().trim());
                ps.executeUpdate();
            } else if (isEditingPayment) {
                LoanPayment selectedPayment = paymentsTable.getSelectionModel().getSelectedItem();
                if (selectedPayment == null) {
                    showAlert(Alert.AlertType.WARNING, "Brak wybranej wpłaty do edycji.");
                    return;
                }

                PreparedStatement ps = conn.prepareStatement("""
                UPDATE dmt.loan_payments 
                SET payment_date=?, amount=?, payment_type=?, note=? 
                WHERE id=?
            """);
                ps.setDate(1, Date.valueOf(paymentDatePicker.getValue()));
                ps.setDouble(2, amountVal);
                ps.setString(3, paymentTypeCombo.getValue());
                ps.setString(4, paymentNoteField.getText().trim());
                ps.setInt(5, selectedPayment.getId());
                ps.executeUpdate();
            }

            setPaymentFormDisabled(true);
            togglePaymentButtons(false);
            isAddingPayment = false;
            isEditingPayment = false;

            loadPayments(selectedLoan.getId());
            recalculateLoanTotals(selectedLoan.getId());

            showAlert(Alert.AlertType.INFORMATION, "Zapisano wpłatę.");
        } catch (NumberFormatException nfe) {
            showAlert(Alert.AlertType.ERROR, "Błędna kwota: " + nfe.getMessage());
            LoggerUtils.logError(nfe);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd zapisu wpłaty: " + e.getMessage());
            LoggerUtils.logError(e);
        }
    }

    private void recalculateLoanTotals(int loanId) {
        try (Connection conn = DBConnectionController.getConnection()) {
            if (conn == null) throw new SQLException("Brak połączenia z bazą danych.");

            PreparedStatement ps = conn.prepareStatement("""
            SELECT 
                COALESCE(SUM(CASE WHEN payment_type = 'rata' THEN amount END), 0) AS total_installments,
                COALESCE(SUM(CASE WHEN payment_type = 'nadpłata' THEN amount END), 0) AS total_overpayments
            FROM dmt.loan_payments
            WHERE loan_id = ?
        """);
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double totalInstallments = rs.getDouble("total_installments");
                double totalOverpayments = rs.getDouble("total_overpayments");
                double totalPaid = totalInstallments + totalOverpayments;

                PreparedStatement updatePs = conn.prepareStatement("""
                UPDATE dmt.loans 
                SET paid_amount = ?, 
                    overpayment_sum = ?, 
                    monthly_payment = ?, 
                    remaining_amount = (total_amount - ?)
                WHERE id = ?
            """);
                updatePs.setDouble(1, totalPaid);
                updatePs.setDouble(2, totalOverpayments);
                updatePs.setDouble(3, totalInstallments);
                updatePs.setDouble(4, totalPaid);
                updatePs.setInt(5, loanId);
                updatePs.executeUpdate();
            }

            loadLoans();

            Loan selected = loansTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fillLoanFields(selected);
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
            showAlert(Alert.AlertType.ERROR, "Błąd podczas przeliczania wartości kredytu: " + e.getMessage());
        }
    }
}
