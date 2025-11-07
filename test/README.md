# K6 Load Test for AccountController

## Overview

This k6 load test script provides professional-grade performance testing for the AccountController API, simulating concurrent deposit and withdraw operations using the new unified transaction endpoint with configurable locking modes.

## Prerequisites

### Install k6

**macOS:**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```powershell
choco install k6
```

Or download from: https://k6.io/docs/get-started/installation/

## Usage

### Basic Usage

```bash
# Run with default settings (50 VUs, 100 iterations)
k6 run -e ACCOUNT_ID=your-account-uuid test/k6-load-test.js
```

### With Custom Parameters

```bash
# Specify base URL, account ID, and locking mode
# Local
k6 run -e BASE_URL=http://localhost:8080  -e ACCOUNT_ID=3f93c1c2-1c52-4df5-8c6a-9b0c6d7c5c11 -e MODE=OPTIMISTIC k6-load-test.js

k6 run -e BASE_URL=http://192.168.56.240  -e ACCOUNT_ID=3f93c1c2-1c52-4df5-8c6a-9b0c6d7c5c11 -e MODE=OPTIMISTIC k6-load-test.js  
```

### Custom Load Patterns

```bash
# High concurrency test: 100 VUs, 500 iterations
k6 run --vus 100 --iterations 500 \
  -e ACCOUNT_ID=your-uuid \
  -e MODE=PESSIMISTIC \
  test/k6-load-test.js

# Duration-based test: 50 VUs for 1 minute
k6 run --vus 50 --duration 1m \
  -e ACCOUNT_ID=your-uuid \
  -e MODE=OPTIMISTIC \
  test/k6-load-test.js

# Ramping test: gradually increase load
k6 run --stage 10s:10,30s:50,10s:0 \
  -e ACCOUNT_ID=your-uuid \
  test/k6-load-test.js
```

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `BASE_URL` | No | `http://localhost:8080` | API base URL |
| `ACCOUNT_ID` | **Yes** | - | Target account UUID |
| `MODE` | No | `OPTIMISTIC` | Locking mode (`OPTIMISTIC` or `PESSIMISTIC`) |

### Test Options

The script includes pre-configured test scenarios:

```javascript
scenarios: {
  concurrent_load: {
    executor: 'shared-iterations',
    vus: 50,              // 50 virtual users
    iterations: 100,      // 100 total iterations
    maxDuration: '2m',    // Maximum 2 minutes
  },
}
```

### Thresholds

Performance criteria for pass/fail:

- **Response Time**: 95% of requests < 2 seconds
- **Error Rate**: < 10% failed requests
- **Version Conflicts**: < 30% conflicts (for OPTIMISTIC mode)

## Features

### 1. **Concurrent Load Testing**
- Simulates multiple users accessing the same account simultaneously
- Tests race conditions and locking behavior
- Configurable virtual users (VUs) and iterations

### 2. **Random Operations**
- Randomly chooses between DEPOSIT and WITHDRAWAL transaction types
- Random amounts between 1-10 per transaction
- Realistic simulation of real-world usage with JSON payloads

### 3. **Custom Metrics**
- `deposits_total`: Total number of deposits
- `withdraws_total`: Total number of withdraws
- `version_conflicts`: Rate of optimistic locking conflicts
- `account_balance`: Trend of account balance over time

### 4. **Detailed Logging**
- Transaction-level logging with timestamps
- Owner name, action, amount, and resulting balance
- Error and conflict detection

### 5. **Initial & Final State**
- Displays account state before test starts
- Shows final state after test completes
- Calculates balance and version changes

## Example Output

```
INFO[0000] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  source=console
INFO[0000] ▶ K6 Load Test for AccountController          source=console
INFO[0000] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  source=console
INFO[0000] ▶ Target base URL : http://192.168.56.240:8080     source=console
INFO[0000] ▶ Account ID      : 3f93c1c2-1c52-4df5-8c6a-9b0c6d7c5c11  source=console
INFO[0000] ▶ Locking Mode    : OPTIMISTIC                source=console
INFO[0000] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  source=console
INFO[0000]                                               source=console
INFO[0000] 📊 Initial Account State:                      source=console
INFO[0000]    Balance: 10 USD | Version: 494 | Owner: Alice  source=console
INFO[0000] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  source=console
INFO[0000]                                               source=console
ERRO[0000] [ERROR 422] WITHDRAWAL 60 failed: Business rule violation - insufficient funds  source=console
INFO[0000] [2025-11-02T13:20:44.705Z] TX:a52d4700-e63a-463c-808e-66bf4132ba26 | DEPOSIT 24 USD | Balance: 34 USD (v494)  source=console                                                                                                                                               
INFO[0000] [2025-11-02T13:20:44.709Z] TX:8d001c33-c64a-44eb-b39f-93ed854c6e02 | DEPOSIT 46 USD | Balance: 159 USD (v496)  source=console
INFO[0000] [2025-11-02T13:20:44.709Z] TX:ba848747-f5ea-4ebf-b1b2-3c501c44cd2b | DEPOSIT 79 USD | Balance: 113 USD (v495)  source=console                                                                                                                                              
INFO[0000] [2025-11-02T13:20:44.723Z] TX:87d8874d-af6c-47a5-9901-823f616e8add | DEPOSIT 10 USD | Balance: 169 USD (v497)  source=console
...
skip
...
INFO[0004] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  
INFO[0004] 📊 Final Account State:                        source=console
INFO[0004]    Balance: 501 USD | Version: 582 | Owner: Alice                                                                                                                                                                                                              
INFO[0004] 📈 Changes:                                    source=console                                                                                                                                                                                                              
INFO[0004]    Balance Change: +491 USD                   source=console                                                                                                                                                                                                               
INFO[0004]    Version Change: +88                        
INFO[0004] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  source=console                                                                                                                                                                                         
INFO[0004] ✅ Test completed successfully!                source=console                                                                                                                                                                                                              


  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<2000' p(95)=1.02s

    http_req_failed
    ✗ 'rate<0.1' rate=11.76%

    version_conflicts
    ✓ 'rate<0.3' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 300    74.544329/s
    checks_succeeded...: 88.00% 264 out of 300
    checks_failed......: 12.00% 36 out of 300

    ✗ status is 200
      ↳  88% — ✓ 88 / ✗ 12
    ✗ response has account data
      ↳  88% — ✓ 88 / ✗ 12
    ✗ response has transaction data
      ↳  88% — ✓ 88 / ✗ 12

    CUSTOM
    account_balance................: avg=499.056818 min=30       med=506     max=983   p(90)=767.9    p(95)=815.95
    deposits_total.................: 48     11.927093/s
    other_errors...................: 12     2.981773/s
    version_conflicts..............: 0.00%  0 out of 0
    withdraws_total................: 52     12.921017/s

    HTTP
    http_req_duration..............: avg=308.79ms   min=10.07ms  med=91.88ms max=2.11s p(90)=900.17ms p(95)=1.02s
      { expected_response:true }...: avg=332.89ms   min=10.07ms  med=99.69ms max=2.11s p(90)=932.24ms p(95)=1.24s
    http_req_failed................: 11.76% 12 out of 102
    http_reqs......................: 102    25.345072/s

    EXECUTION
    iteration_duration.............: avg=1.5s       min=578.54ms med=1.48s   max=3.79s p(90)=2.12s    p(95)=2.51s
    iterations.....................: 100    24.84811/s
    vus............................: 1      min=1         max=50
    vus_max........................: 50     min=50        max=50

    NETWORK
    data_received..................: 94 kB  23 kB/s
    data_sent......................: 28 kB  6.9 kB/s
                                                                                                                                                                                                                                                                                      
running (0m04.0s), 00/50 VUs, 100 complete and 0 interrupted iterations                                                                                                                                                                                                               
concurrent_load ✓ [======================================] 50 VUs  0m04.0s/2m0s  100/100 shared iters                                                                                                                                                                                 
```

## Testing Scenarios

### 1. Test Optimistic Locking Under Load

```bash
k6 run --vus 50 --iterations 200 \
  -e ACCOUNT_ID=your-uuid \
  -e MODE=OPTIMISTIC \
  test/k6-load-test.js
```

**Expected Behavior:**
- Some version conflicts (retries)
- High throughput
- Lower latency

### 2. Test Pessimistic Locking Under Load

```bash
k6 run --vus 50 --iterations 200 \
  -e ACCOUNT_ID=your-uuid \
  -e MODE=PESSIMISTIC \
  test/k6-load-test.js
```

**Expected Behavior:**
- No version conflicts
- Serialized access
- Higher latency due to locking


Compare metrics like:
- Average response time
- P95 response time
- Throughput (requests/second)
- Error rate
- Version conflict rate

## Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Examples](https://k6.io/docs/examples/)
