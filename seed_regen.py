#!/usr/bin/env python3
"""Regenerate the embedded SEED in app/index.html from job_tracker.csv.

Usage: python3 seed_regen.py [csv_path] [index_html_path]
Defaults: jobs/job_tracker.csv  app/index.html   (relative to repo root)
Handles the three historical CSV row layouts (see AGENTS.md). Never use DictReader here.
"""
import csv, json, re, sys, os

ROOT = os.path.dirname(os.path.abspath(__file__))
CSV = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'jobs', 'job_tracker.csv')
HTML = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, 'app', 'index.html')
LOC = re.compile(r'^(abu dhabi|dubai|al ain|al-ain|sharjah|ajman|ras al|umm al|fujairah|uae|u\.a\.e|united arab|masdar)', re.I)

def map_status(s):
    u = (s or '').upper()
    if 'APPLIED' in u: return 'applied'
    if 'SHOWN' in u: return 'shown'
    if 'WATCHLIST' in u: return 'watchlist'
    if 'CLOSED' in u: return 'closed'
    return 'reference'

rows = list(csv.reader(open(CSV, encoding='utf-8')))[1:]
seed = []
for i, f in enumerate(rows):
    n = len(f); div = ''; posted = ''
    clean = lambda x: (x or '').strip()
    link = resume = ''
    if n >= 11 and (clean(f[7]).startswith('Mansoor') or clean(f[7]) == ''):
        date, title, company, div, loc, posted, match, resume, link, status = (clean(x) for x in f[0:10])
        notes = ','.join(x for x in map(clean, f[10:]) if x)
    elif n == 10 and LOC.match(clean(f[2])) and not LOC.match(clean(f[3])):
        date, title, loc, company, summ, req, match, status = (clean(x) for x in f[0:8])
        extra = []
        for x in f[8:]:
            xs = clean(x)
            if xs.startswith(('http', 'mailto')): link = xs
            elif xs.startswith('Mansoor_Resume'): resume = xs
            elif xs: extra.append(xs)
        notes = ' | '.join(p for p in [summ, ('Reqs: ' + req) if req else ''] + extra if p)
    else:
        date, title, company, div, loc, req, match, status = (clean(x) for x in f[0:8])
        extra = []
        for x in f[8:]:
            xs = clean(x)
            if xs.startswith(('http', 'mailto')): link = xs
            elif xs.startswith('Mansoor_Resume'): resume = xs
            elif xs: extra.append(xs)
        notes = ' | '.join(p for p in ([('Reqs/notes: ' + req) if req else ''] + extra) if p)
    m = int(re.sub(r'\D', '', match or '') or 0)
    st = map_status(status)
    if resume.endswith('.docx'): resume = resume[:-5] + '.pdf'
    seed.append({'id': 'seed-%d' % i, 'company': company, 'title': title, 'division': div,
                 'location': loc, 'link': link, 'match': m, 'resume': resume,
                 'shown': date, 'posted': posted, 'status': st, 'rawStatus': status,
                 'notes': notes, 'appliedDate': date if st == 'applied' else '',
                 'followUps': [], 'myNotes': ''})

html = open(HTML, encoding='utf-8').read()
new = re.sub(r'const SEED = \[.*?\];\nconst LSKEY',
             'const SEED = ' + json.dumps(seed, ensure_ascii=False) + ';\nconst LSKEY',
             html, count=1, flags=re.S)
if new == html:
    sys.exit('ERROR: SEED marker not found in %s' % HTML)
open(HTML, 'w', encoding='utf-8').write(new)
from collections import Counter
print('seed rows:', len(seed), dict(Counter(s['status'] for s in seed)))
print('patched:', HTML)
