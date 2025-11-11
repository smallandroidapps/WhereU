package com.whereu.whereu.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.whereu.whereu.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlansActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private ProductDetails monthlyDetails;
    private ProductDetails yearlyDetails;
    private ProductDetails lifetimeDetails;

    private RadioGroup planRadioGroup;
    private RadioButton radioMonthly;
    private RadioButton radioYearly;
    private RadioButton radioLifetime;
    private Button btnContinue;

    private static final String PREFS_NAME = "wheru_prefs";
    private static final String PREF_USER_PLAN = "userPlan"; // Values: MONTHLY, YEARLY, LIFETIME

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        planRadioGroup = findViewById(R.id.plan_radio_group);
        radioMonthly = findViewById(R.id.radio_monthly);
        radioYearly = findViewById(R.id.radio_yearly);
        radioLifetime = findViewById(R.id.radio_lifetime);
        btnContinue = findViewById(R.id.btn_continue);

        setupCardClicks();

        // Default select Lifetime plan
        selectPlan("LIFETIME");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        connectBilling();

        // Redirect to payment options screen with selected plan and amount
        btnContinue.setOnClickListener(v -> {
            if (selectedPlanTag == null) {
                Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
                return;
            }
            String amount = getAmountForPlan(selectedPlanTag);
            Intent i = new Intent(this, PaymentOptionsActivity.class);
            i.putExtra(PaymentOptionsActivity.EXTRA_PLAN_TAG, selectedPlanTag);
            i.putExtra(PaymentOptionsActivity.EXTRA_AMOUNT, amount);
            startActivity(i);
        });
    }

    private void setupCardClicks() {
        findViewById(R.id.card_monthly).setOnClickListener(v -> selectPlan("MONTHLY"));
        findViewById(R.id.card_yearly).setOnClickListener(v -> selectPlan("YEARLY"));
        findViewById(R.id.card_lifetime).setOnClickListener(v -> selectPlan("LIFETIME"));

        radioMonthly.setOnClickListener(v -> selectPlan("MONTHLY"));
        radioYearly.setOnClickListener(v -> selectPlan("YEARLY"));
        radioLifetime.setOnClickListener(v -> selectPlan("LIFETIME"));
    }

    private void selectPlan(String planTag) {
        selectedPlanTag = planTag;
        // Ensure exclusive selection since radios are nested inside cards
        radioMonthly.setChecked("MONTHLY".equals(planTag));
        radioYearly.setChecked("YEARLY".equals(planTag));
        radioLifetime.setChecked("LIFETIME".equals(planTag));
    }

    private String getAmountForPlan(String planTag) {
        switch (planTag) {
            case "MONTHLY": return "10";
            case "YEARLY": return "100";
            case "LIFETIME": return "2000";
            default: return "0";
        }
    }

    private void connectBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                } else {
                    Toast.makeText(PlansActivity.this, "Billing not ready", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Retry connection
                connectBilling();
            }
        });
    }

    private void queryProducts() {
        // Query SUBS (monthly, yearly)
        List<QueryProductDetailsParams.Product> subs = new ArrayList<>();
        subs.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("wheru_monthly_10")
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        subs.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("wheru_yearly_100")
                .setProductType(BillingClient.ProductType.SUBS)
                .build());

        QueryProductDetailsParams subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subs)
                .build();

        billingClient.queryProductDetailsAsync(subsParams, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (ProductDetails pd : productDetailsList) {
                    if ("wheru_monthly_10".equals(pd.getProductId())) {
                        monthlyDetails = pd;
                    } else if ("wheru_yearly_100".equals(pd.getProductId())) {
                        yearlyDetails = pd;
                    }
                }
            }
        });

        // Query INAPP (lifetime)
        List<QueryProductDetailsParams.Product> inapp = Collections.singletonList(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("wheru_lifetime_2000")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams inappParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inapp)
                .build();

        billingClient.queryProductDetailsAsync(inappParams, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (ProductDetails pd : productDetailsList) {
                    if ("wheru_lifetime_2000".equals(pd.getProductId())) {
                        lifetimeDetails = pd;
                    }
                }
            }
        });
    }

    private void startSelectedBillingFlow() {
        if (radioMonthly.isChecked() && monthlyDetails != null) {
            startBillingFlowForProduct(monthlyDetails, BillingClient.ProductType.SUBS, "MONTHLY");
        } else if (radioYearly.isChecked() && yearlyDetails != null) {
            startBillingFlowForProduct(yearlyDetails, BillingClient.ProductType.SUBS, "YEARLY");
        } else if (radioLifetime.isChecked() && lifetimeDetails != null) {
            startBillingFlowForProduct(lifetimeDetails, BillingClient.ProductType.INAPP, "LIFETIME");
        } else {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
        }
    }

    private void continueWithDummyPayment() {
        if (selectedPlanTag == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
            return;
        }
        // Simulate payment success and upgrade
        Toast.makeText(this, "Processing payment...", Toast.LENGTH_SHORT).show();
        saveUserPlan(selectedPlanTag);
        writeProStatusToFirestore(selectedPlanTag, null);
        enablePremiumFeatures();
        Toast.makeText(this, "Pro activated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void startBillingFlowForProduct(ProductDetails productDetails, @BillingClient.ProductType String type, String planTag) {
        BillingFlowParams.ProductDetailsParams.Builder pdBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (BillingClient.ProductType.SUBS.equals(type)) {
            // Choose the first available offer token
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                pdBuilder.setOfferToken(offers.get(0).getOfferToken());
            }
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdBuilder.build()))
                .build();

        int response = billingClient.launchBillingFlow(this, flowParams).getResponseCode();
        if (response != BillingClient.BillingResponseCode.OK) {
            Toast.makeText(this, "Unable to start purchase", Toast.LENGTH_SHORT).show();
        }
        // Store selected plan intent for saving on purchase update
        selectedPlanTag = planTag;
    }

    private String selectedPlanTag = null;

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Purchase canceled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Purchase error", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        // TODO: Verify purchase with your backend if applicable.
        // Acknowledge if required (non-consumable, subs)
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(params, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    saveUserPlan(selectedPlanTag);
                    writeProStatusToFirestore(selectedPlanTag, purchase);
                    enablePremiumFeatures();
                    Toast.makeText(PlansActivity.this, "Pro activated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void saveUserPlan(String plan) {
        if (plan == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_USER_PLAN, plan).apply();
    }

    public static boolean isProUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String plan = prefs.getString(PREF_USER_PLAN, null);
        return plan != null; // MONTHLY, YEARLY, or LIFETIME
    }

    private void enablePremiumFeatures() {
        // TODO: Hook into app feature flags to unlock Pro capabilities
    }

    private void writeProStatusToFirestore(String planType, Purchase purchase) {
        try {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Log.w("PlansActivity", "No user; skipping Firestore pro write");
                return;
            }
            String userId = user.getUid();
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("isPro", true);
            updates.put("planType", planType);
            updates.put("proSince", FieldValue.serverTimestamp());
            updates.put("lastUpdated", FieldValue.serverTimestamp());
            // Optional audit fields when purchase is available
            if (purchase != null) {
                updates.put("purchaseToken", purchase.getPurchaseToken());
                updates.put("purchaseProducts", purchase.getProducts());
            }

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Log.d("PlansActivity", "Pro status written to Firestore"))
                    .addOnFailureListener(e -> Log.e("PlansActivity", "Failed to write pro status: " + e.getMessage()));
        } catch (Exception e) {
            Log.e("PlansActivity", "Error writing pro status", e);
        }
    }
}
