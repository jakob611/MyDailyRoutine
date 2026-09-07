"""Read the simple Room entity declarations to exercise their DDL without an Android SDK.
Not a substitute for Room's generated schema: Android migration tests validate that independently.
"""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[1]
SOURCE = '\n'.join(p.read_text() for p in sorted((ROOT / 'app/src/main/java/com/example/mydailyroutine/core/database/entities').glob('*.kt')))
MATCHES = list(re.finditer(r'data class (\w+)\(', SOURCE))
ENTITIES = {}
for i,m in enumerate(MATCHES):
    meta=SOURCE[SOURCE.rfind('@Entity',0,m.start()):m.start()]
    end=SOURCE.rfind('@Entity',m.end(),MATCHES[i+1].start()) if i+1<len(MATCHES) else len(SOURCE)
    body=SOURCE[m.end():end]
    table=re.search(r'tableName\s*=\s*"(\w+)"',meta).group(1)
    columns=[]; last=0
    for col in re.finditer(r'val (\w+): ([\w?]+)',body):
        prefix=body[last:col.start()]; last=col.end()
        annotation=re.findall(r'@ColumnInfo\(([^)]*)\)',prefix)
        anno=annotation[-1] if annotation else ''
        name=re.search(r'name\s*=\s*"([^"]+)"',anno)
        default=re.search(r'defaultValue\s*=\s*"([^"]+)"',anno)
        columns.append((name.group(1) if name else col.group(1),col.group(2),default.group(1) if default else None,
                        (2 if 'autoGenerate = true' in prefix else 1) if '@PrimaryKey' in prefix else 0))
    ENTITIES[m.group(1)]=(table,meta,columns)

def quoted(text): return re.findall(r'"(\w+)"',text)
def ddl():
    result={}
    for table,meta,cols in ENTITIES.values():
        definitions=[]
        for name,kind,default,primary in cols:
            bare=kind.rstrip('?')
            affinity='TEXT' if bare in ('String','RoutineCategory','CancellationReason') else 'REAL' if bare=='Double' else 'INTEGER'
            value=f'`{name}` {affinity}'
            if primary: value+=' PRIMARY KEY'+(' AUTOINCREMENT' if primary==2 else '')
            if not kind.endswith('?'): value+=' NOT NULL'
            if default is not None: value+=' DEFAULT '+default
            definitions.append(value)
        pk=re.search(r'primaryKeys\s*=\s*\[([^]]+)\]',meta)
        if pk: definitions.append('PRIMARY KEY('+','.join('`'+n+'`' for n in quoted(pk.group(1)))+')')
        for fk in re.findall(r'ForeignKey\((.*?)\)',meta,re.S):
            parent=ENTITIES[re.search(r'entity\s*=\s*(\w+)::class',fk).group(1)][0]
            parents=quoted(re.search(r'parentColumns\s*=\s*\[([^]]+)\]',fk).group(1))
            children=quoted(re.search(r'childColumns\s*=\s*\[([^]]+)\]',fk).group(1))
            delete=re.search(r'onDelete\s*=\s*ForeignKey\.(\w+)',fk).group(1).replace('_',' ')
            definitions.append(f'FOREIGN KEY({",".join(children)}) REFERENCES `{parent}`({",".join(parents)}) ON DELETE {delete} ON UPDATE NO ACTION')
        statements=[f'CREATE TABLE IF NOT EXISTS `{table}` ({", ".join(definitions)})']
        for ix in re.findall(r'Index\(([^)]+)\)',meta):
            names=quoted(ix); unique='UNIQUE ' if 'unique = true' in ix else ''
            statements.append(f'CREATE {unique}INDEX IF NOT EXISTS `index_{table}_{"_".join(names)}` ON `{table}`({",".join(names)})')
        result[table]=statements
    return result
