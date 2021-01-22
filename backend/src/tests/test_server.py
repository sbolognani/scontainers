import unittest


class MyTest(unittest.TestCase):
    def test_equal_numbers(self):
        self.assertEqual(2, 2)


if __name__ == '__main__':
    unittest.main()
