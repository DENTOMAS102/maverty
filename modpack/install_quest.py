#!/usr/bin/env python3
"""Adds the Goddess of Death to the Wild Blood quest tree ("Угроза Лордов").

    python3 modpack/install_quest.py <path to the game folder, or to the chapter .snbt file>

She appears as a lone eye at the top of the tree, above the End: the title and the description are
struck out, and the quest completes when she is killed. Running it twice changes nothing. The
original chapter file is kept next to it with a .bak extension.
"""
import glob
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
QUEST_ID = '0DEA7460DDE55001'


def find_chapter(path):
    if os.path.isfile(path):
        return path
    chapters = sorted(glob.glob(os.path.join(path, '**', 'ftbquests', 'quests', 'chapters', '*.snbt'), recursive=True))
    if not chapters:
        sys.exit(f'No FTB Quests chapters found under {path}')
    for chapter in chapters:
        if 'Угроза Лордов' in open(chapter, encoding='utf-8').read():
            return chapter
    return chapters[0]


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    chapter = find_chapter(sys.argv[1])
    raw = open(chapter, 'rb').read()
    newline = '\r\n' if raw.count(b'\r\n') > raw.count(b'\n') // 2 else '\n'
    lines = raw.decode('utf-8').replace('\r\n', '\n').split('\n')
    if any(QUEST_ID in line for line in lines):
        print(f'{chapter}: the quest is already there, nothing to do')
        return
    # The quest list is the block that closes with a single-tab "]" just before the chapter title.
    closing = max(i for i, line in enumerate(lines) if line == '\t]' and i + 1 < len(lines) and lines[i + 1].startswith('\ttitle:'))
    if lines[closing - 1] != '\t\t}':
        sys.exit(f'{chapter}: unexpected layout, not touching it')
    quest = open(os.path.join(HERE, 'ftbquests', 'goddess_quest.snbt'), encoding='utf-8').read().rstrip('\n').split('\n')
    lines[closing:closing] = quest
    shutil.copyfile(chapter, chapter + '.bak')
    with open(chapter, 'wb') as f:
        f.write(newline.join(lines).encode('utf-8'))
    print(f'{chapter}: added the quest ({len(quest)} lines); backup at {os.path.basename(chapter)}.bak')


if __name__ == '__main__':
    main()
