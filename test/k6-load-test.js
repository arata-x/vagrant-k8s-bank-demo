/**
 * K6 Load Test for AccountController
 * 
 * This script tests the AccountController API with configurable locking modes,
 * simulating concurrent deposit and withdraw operations using the new transaction endpoint.
 * 
 * Usage:
 *   k6 run test/k6-load-test.js
 *   k6 run -e BASE_URL=http://localhost:8080 -e ACCOUNT_ID=uuid -e MODE=OPTIMISTIC test/k6-load-test.js
 *   k6 run --vus 50 --duration 30s test/k6-load-test.js
 * 
 * Environment Variables:
 *   BASE_URL    : API base URL (default: http://localhost:8080)
 *   ACCOUNT_ID  : Target account UUID (required)
 *   MODE        : Locking mode - OPTIMISTIC or PESSIMISTIC (default: OPTIMISTIC)
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

// ============================================================================
// Configuration
// ============================================================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCOUNT_ID = __ENV.ACCOUNT_ID;
const MODE = __ENV.MODE || 'OPTIMISTIC';
const params = {
  headers: {
    "Host": "app.demo.local",
    "Content-Type": "application/json",
  },
};

// Validate required parameters
if (!ACCOUNT_ID) {
  throw new Error('ACCOUNT_ID environment variable is required. Usage: k6 run -e ACCOUNT_ID=your-uuid test/k6-load-test.js');
}

if (MODE !== 'OPTIMISTIC' && MODE !== 'PESSIMISTIC') {
  throw new Error(`MODE must be 'OPTIMISTIC' or 'PESSIMISTIC', got: ${MODE}`);
}

// K6 test options
export const options = {
  // Scenario 1: Concurrent load test
  scenarios: {
    concurrent_load: {
      executor: 'shared-iterations',
      vus: 50,              // 50 virtual users
      iterations: 100,      // 100 total iterations
      maxDuration: '2m',    // Maximum 2 minutes
    },
  },
  
  // Thresholds for pass/fail criteria
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95% of requests should be below 2s
    http_req_failed: ['rate<0.1'],      // Less than 10% of requests should fail
    'version_conflicts': ['rate<0.3'],  // Less than 30% version conflicts (for OPTIMISTIC)
  },
};

// ============================================================================
// Custom Metrics
// ============================================================================

const depositCounter = new Counter('deposits_total');
const withdrawCounter = new Counter('withdraws_total');
const versionConflicts = new Rate('version_conflicts');
const validationErrors = new Counter('validation_errors');
const otherErrors = new Counter('other_errors');
const balanceTrend = new Trend('account_balance');

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Randomly select deposit or withdraw action
 */
function getRandomAction() {
  return Math.random() < 0.5 ? 'DEPOSIT' : 'WITHDRAWAL';
}

/**
 * Generate random amount between 1 and 10
 */
function getRandomAmount() {
  return Math.floor(Math.random() * 100) + 1;
}

/**
 * Get current account state
 */
function getAccountState() {
  const url = `${BASE_URL}/api/accounts/${ACCOUNT_ID}`;
  const response = http.get(url, params);
  
  if (response.status === 200) {
    const data = JSON.parse(response.body).data;
    return {
      balance: data.balance,
      version: data.version,
      owner: data.ownerName,
      currency: data.currency,
    };
  }
  
  return null;
}

/**
 * Perform a transaction operation (deposit or withdrawal)
 */
function performTransaction(type, amount, reason) {
  const url = `${BASE_URL}/api/accounts/${ACCOUNT_ID}/transaction`;
  
  const payload = JSON.stringify({
    type: type,
    amount: amount,
    lockingMode: MODE,
    reason: reason
  });
  
  const response = http.post(url, payload, params);
  
  // Update counters based on transaction type
  if (type === 'DEPOSIT') {
    depositCounter.add(1);
  } else {
    withdrawCounter.add(1);
  }
  
  return {
    response,
    action: type.toLowerCase(),
    amount,
  };
}

/**
 * Log transaction result with formatted output
 */
function logTransaction(action, amount, accountData, transactionId) {
  const timestamp = new Date().toISOString();
  console.log(
    `[${timestamp}] TX:${transactionId} | ${action.toUpperCase()} ${amount} ${accountData.currency} | ` +
    `Balance: ${accountData.balance} ${accountData.currency} (v${accountData.version})`
  );
}

/**
 * Parse and log error response
 */
function logError(status, action, amount, responseBody) {
  try {
    const errorBody = JSON.parse(responseBody);
    const message = errorBody.message || errorBody.error || responseBody;
    console.error(`[ERROR ${status}] ${action} ${amount} failed: ${message}`);
  } catch (e) {
    console.error(`[ERROR ${status}] ${action} ${amount} failed: ${responseBody}`);
  }
}

// ============================================================================
// Setup and Teardown
// ============================================================================

/**
 * Setup function - runs once before the test
 */
export function setup() {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('▶ K6 Load Test for AccountController');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`▶ Target base URL : ${BASE_URL}`);
  console.log(`▶ Account ID      : ${ACCOUNT_ID}`);
  console.log(`▶ Locking Mode    : ${MODE}`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('');
  
  // Get initial account state
  const initialState = getAccountState();
  
  if (initialState) {
    console.log('📊 Initial Account State:');
    console.log(`   Balance: ${initialState.balance} ${initialState.currency} | ` +
                `Version: ${initialState.version} | Owner: ${initialState.owner}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('');
  }
  
  return initialState;
}

/**
 * Teardown function - runs once after the test
 */
export function teardown(data) {
  console.log('');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  
  // Get final account state
  const finalState = getAccountState();
  
  if (finalState) {
    console.log('📊 Final Account State:');
    console.log(`   Balance: ${finalState.balance} ${finalState.currency} | ` + `Version: ${finalState.version} | Owner: ${finalState.owner}`);
    
    if (data) {
      const balanceChange = finalState.balance - data.balance;
      const versionChange = finalState.version - data.version;
      console.log('');
      console.log('📈 Changes:');
      console.log(`   Balance Change: ${balanceChange > 0 ? '+' : ''}${balanceChange} ${finalState.currency}`);
      console.log(`   Version Change: +${versionChange}`);
    }
  }
  
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('✅ Test completed successfully!');
}

// ============================================================================
// Main Test Function
// ============================================================================

/**
 * Default function - runs for each iteration
 * 
 */
export default function () {
  group('Account Transaction', function () {
    // Randomly choose action and amount
    const action = getRandomAction();
    const amount = getRandomAmount();
    const reason = `${action}_${MODE}`;
    
    // Perform the transaction
    const result = performTransaction(action, amount, reason);
    const { response } = result;
    
    // Check response status and structure
    const success = check(response, {
      'status is 200': (r) => r.status === 200,
      'response has account data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.account && body.account.id !== undefined;
        } catch (e) {
          return false;
        }
      },
      'response has transaction data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.transactionId !== undefined;
        } catch (e) {
          return false;
        }
      },
    });
    
    // Process response based on status code
    if (success && response.status === 200) {
      try {
        const body = JSON.parse(response.body);
        
        // Validate required fields
        if (!body.account || !body.transactionId) {
          console.error(`[VALIDATION] Missing required fields in response`);
          validationErrors.add(1);
          return;
        }
        
        const accountData = body.account;
        
        // Record metrics
        balanceTrend.add(accountData.balance);
        
        // Log transaction using helper function
        logTransaction(action, amount, accountData, body.transactionId);
        
      } catch (e) {
        console.error(`[PARSE ERROR] Failed to parse response: ${e.message}`);
        otherErrors.add(1);
      }
      
    } else if (response.status === 409) {
      // Version conflict (optimistic locking)
      versionConflicts.add(1);
      console.log(`[CONFLICT] Version conflict for ${action} ${amount} - will be retried by application`);
      
    } else if (response.status === 400) {
      // Bad request (validation error)
      validationErrors.add(1);
      logError(400, action, amount, response.body);
      
    } else {
      // Other errors
      otherErrors.add(1);
      logError(response.status, action, amount, response.body);
    }
  });
  
  // Add realistic think time between requests (0.5-2 seconds)
  // This simulates real user behavior and prevents overwhelming the server
  sleep(Math.random() * 1.5 + 0.5);
}
