import json,pathlib
here=pathlib.Path(__file__).parent
cat=(here.parent/'tools'/'catalog.web.json').read_text(encoding='utf-8')
src=(here/'planner.src.html').read_text(encoding='utf-8')
out=src.replace('__CATALOG__',cat)
(here/'citybuilder-planner.html').write_text(out,encoding='utf-8')
print(len(out))
