# Test Data Generator

## How to Generate Test Data

### Method 1: Using the Hidden Easter Egg (Recommended)
1. Open the app and navigate to the Login screen
2. **Tap the "Sign In" title 5 times** quickly
3. A toast message will appear: "Generating test data..."
4. After generation completes, you'll see: "Test data created! Email: test@finance.com Password: Test123"
5. The login fields will be automatically filled with the test credentials
6. Click LOGIN to access the app with test data

### Method 2: Manual Code Call
In any Activity where you have a Context, you can call:
```java
TestDataGenerator.generateTestData(context);
```

## What Test Data is Generated

### Test User
- **Email**: test@finance.com
- **Password**: Test123
- **Full Name**: Test User

### Transactions
The generator creates realistic financial data for **March through December 2025** (10 months):

#### Income (1-2 per month)
- Random amounts between **$2,000 - $5,000**
- Categories: Salary, Freelance, Investment, Other
- Total: ~10-20 income transactions

#### Expenses (8-15 per month)
- Random amounts between **$20 - $520**
- Categories:
  - Food & Dining
  - Transportation
  - Shopping
  - Entertainment
  - Bills & Utilities
  - Healthcare
  - Education
  - Other
- Total: ~80-150 expense transactions

### Budgets
5 pre-configured budgets:
1. **Food & Dining**: $800/month
2. **Transportation**: $400/month
3. **Shopping**: $600/month
4. **Entertainment**: $300/month
5. **Bills & Utilities**: $500/month

## Testing the App Features

After logging in with the test account, you can:

1. **Home Dashboard**: View total income/expenses, monthly summaries, and pie charts
2. **Income Tab**: See all income transactions sorted by date
3. **Expenses Tab**: Browse expense transactions with filtering
4. **Budgets Tab**: Check budget usage with progress bars and alerts
5. **Statistics Dashboard**: Analyze comprehensive financial statistics:
   - Month-over-month comparison
   - 6-month trend charts
   - Category breakdown
   - Savings rate calculation
   - Top spending categories

## Data Characteristics

- **Reproducible**: Uses fixed random seed (12345), so data is consistent across runs
- **Realistic**: Amounts and frequencies mimic real-world spending patterns
- **Diverse**: Multiple categories ensure all app features are tested
- **Time-ranged**: 10 months of data provides meaningful chart visualizations
- **Budget-aware**: Budgets are set to realistic limits that will show various alert states

## Clearing Test Data

To remove test data:
1. Uninstall and reinstall the app, OR
2. Clear app data in Android Settings > Apps > Finance App > Storage > Clear Data

## Notes

- The test data generator can be run multiple times, but it will create duplicate data
- For best results, start with a fresh app installation
- All amounts use decimal precision (e.g., $45.67)
- Dates are distributed evenly across the specified month range
