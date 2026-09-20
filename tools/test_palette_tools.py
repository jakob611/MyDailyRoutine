"""Regression tests for the math/parser used by the contrast gate itself."""
import unittest
from palette_tools import palette, ratio, composite, glass_styles


class PaletteToolsTest(unittest.TestCase):
    def test_wcag_reference_pairs(self):
        self.assertAlmostEqual(21.0, ratio('#FFFFFF', '#000000'))
        self.assertAlmostEqual(1.0, ratio('#123456', '#123456'))
        self.assertAlmostEqual(ratio('#A8B3C2', '#090D16'), ratio('#090D16', '#A8B3C2'))

    def test_compositing_endpoints(self):
        self.assertEqual('#000000', composite('#FFFFFF', 0, '#000000'))
        self.assertEqual('#FFFFFF', composite('#FFFFFF', 1, '#000000'))
        self.assertEqual('#808080', composite('#FFFFFF', .5, '#000000'))

    def test_alias_and_category_parsing(self):
        colors, categories = palette()
        self.assertEqual(colors['SurfaceHighest'], colors['SheetSurface'])
        self.assertEqual(colors['FocusAccent'], categories['Focus'][0])
        self.assertEqual(colors['TextPrimary'], categories['Focus'][2])

    def test_translucent_brief_needs_readability_exception(self):
        colors, _ = palette()
        self.assertLess(ratio(colors['TextSecondary'],
                              composite(colors['SurfaceLow'], .68, '#FFFFFF')), 4.5)
        styles, roles, _ = glass_styles()
        self.assertEqual(4, len(roles))
        self.assertGreaterEqual(ratio(colors['TextSecondary'],
                                      composite(colors['SurfaceLow'], styles['Bar']['alpha'], '#FFFFFF')), 4.5)


if __name__ == '__main__':
    unittest.main()
