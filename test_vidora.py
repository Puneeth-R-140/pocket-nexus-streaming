import urllib.request
import urllib.error
import ssl

url = "https://vidora.su/movie/550"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Referer': 'https://vidora.su/',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8'
}

# Create SSL context to ignore certificate errors
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

try:
    print(f"Testing {url}...")
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, context=ctx, timeout=10) as resp:
        print(f"Status: {resp.status}")
        content = resp.read().decode('utf-8', errors='ignore')
        print(f"Response length: {len(content)}")
        print(f"Response preview:\n{content[:500]}")
        
        # Check for specific patterns
        if "workers.dev" in content:
            print("FOUND: workers.dev link")
        if "m3u8" in content:
            print("FOUND: m3u8 link")
            
except Exception as e:
    print(f"Error: {e}")
