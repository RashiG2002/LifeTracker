import os
import re

templates_dir = r"c:\Users\CyB3R\Documents\GitHub\LifeTrackerITp\src\main\resources\templates"
files_to_fix = ['dashboard.html', 'transactions.html', 'budgets.html', 'health.html', 'skills.html', 'reports.html']

for file in files_to_fix:
    path = os.path.join(templates_dir, file)
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove lang-switcher form
        content = re.sub(r'<form class="lang-switcher".*?</form>', '', content, flags=re.DOTALL)
        
        # Replace sidebar header
        content = content.replace('<h1><i class="fas fa-leaf"></i> LifeTracker</h1>', '<h1><div class="logo-badge">LT</div> LifeTracker</h1>')
        
        # Remove th:lang tag
        content = content.replace('th:lang="${#locale.language}"', '')
        
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Fixed {file}")
