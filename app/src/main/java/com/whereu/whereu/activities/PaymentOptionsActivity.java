package com.whereu.whereu.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.content.SharedPreferences;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.adapters.SavedCardsAdapter;
import com.whereu.whereu.models.SavedCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentOptionsActivity extends AppCompatActivity implements SavedCardsAdapter.OnSavedCardSelectedListener {

    public static final String EXTRA_PLAN_TAG = "plan_tag";
    public static final String EXTRA_AMOUNT = "amount"; // in INR
    private static final String UPI_ID = "ganeshyelagandula426@oksbi";
    private static final int RC_UPI = 9911;

    private String planTag;
    private String amount;

    private TextView amountText;
    private ImageView btnGPay;
    private ImageView btnPhonePe;
    private ImageView btnUpiOther;
    private Button btnMarkPaid;
    private RecyclerView savedCardsRecycler;
    private SavedCardsAdapter savedCardsAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_options);

        planTag = getIntent().getStringExtra(EXTRA_PLAN_TAG);
        amount = getIntent().getStringExtra(EXTRA_AMOUNT);

        amountText = findViewById(R.id.amount_text);
        btnGPay = findViewById(R.id.btn_gpay);
        btnPhonePe = findViewById(R.id.btn_phonepe);
        btnUpiOther = findViewById(R.id.btn_upi_other);
        btnMarkPaid = findViewById(R.id.btn_mark_paid);
        savedCardsRecycler = findViewById(R.id.saved_cards_recycler);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        amountText.setText("Amount: ₹" + (amount != null ? amount : "-") + " (" + (planTag != null ? planTag : "-") + ")");

        setupSavedCards();

        btnGPay.setOnClickListener(v -> launchUpiIntent("com.google.android.apps.nbu.paisa.user"));
        btnPhonePe.setOnClickListener(v -> launchUpiIntent("com.phonepe.app"));
        btnUpiOther.setOnClickListener(v -> launchUpiIntent(null));

        btnMarkPaid.setOnClickListener(v -> markUpgradeAfterDummyPayment());
    }

    private void setupSavedCards() {
        // Fetch user's saved cards from Firestore (masked/demo)
        FirebaseUser user = mAuth.getCurrentUser();
        List<SavedCard> cards = new ArrayList<>();
        savedCardsAdapter = new SavedCardsAdapter(cards, this);
        savedCardsRecycler.setAdapter(savedCardsAdapter);
        if (user == null) return;
        db.collection("users").document(user.getUid()).collection("payment_cards").get()
                .addOnSuccessListener(snap -> {
                    cards.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        SavedCard c = d.toObject(SavedCard.class);
                        if (c != null) cards.add(c);
                    }
                    savedCardsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // No cards or error; keep empty UI
                });
    }

    @Override
    public void onSavedCardSelected(SavedCard card) {
        Toast.makeText(this, "Selected card • " + card.getMasked(), Toast.LENGTH_SHORT).show();
    }

    private void launchUpiIntent(@Nullable String packageName) {
        if (amount == null || amount.isEmpty()) {
            Toast.makeText(this, "Missing amount", Toast.LENGTH_SHORT).show();
            return;
        }
        String payeeName = "WherU";
        String txnNote = "WherU Pro " + (planTag != null ? planTag : "");
        String uri = "upi://pay?pa=" + UPI_ID +
                "&pn=" + Uri.encode(payeeName) +
                "&tn=" + Uri.encode(txnNote) +
                "&am=" + amount +
                "&cu=INR";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        try {
            startActivityForResult(intent, RC_UPI);
        } catch (Exception e) {
            Toast.makeText(this, "Payment app not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_UPI) {
            Toast.makeText(this, "Returned from payment app", Toast.LENGTH_SHORT).show();
            // Many UPI apps don't give reliable status back; allow manual confirmation
        }
    }

    private void markUpgradeAfterDummyPayment() {
        if (planTag == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
            return;
        }
        saveUserPlan(planTag);
        writeProStatusToFirestore(planTag);
        Toast.makeText(this, "Pro activated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveUserPlan(String plan) {
        SharedPreferences prefs = getSharedPreferences("wheru_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("userPlan", plan).apply();
    }

    private void writeProStatusToFirestore(String planType) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isPro", true);
        updates.put("planType", planType);
        updates.put("proSince", FieldValue.serverTimestamp());
        updates.put("lastUpdated", FieldValue.serverTimestamp());
        db.collection("users").document(userId).update(updates);
    }
}
