$jobs = @()
for ($i = 1; $i -le 10; $i++) {
    $job = Start-Job -ScriptBlock {
        while ($true) {
            try {
                Invoke-RestMethod -Uri "http://172.16.1.131:8080/text-tone" -Method Get -TimeoutSec 5
            } catch {
            }
            Start-Sleep -Milliseconds 100
        }
    }
    $jobs += $job
}

while ($true) {
    Start-Sleep 1
}