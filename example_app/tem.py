order = '1 gram sos, 400 gram ket, 3 gram dmt, 4 4fmp, 10 4 fmp'

PRODUCTS = ['gram sos', 'gram ket', 'gram dmt', '4fmp']

# TODO Levenshtein helper


def order_parser(order: str):
    amounts, matches = [], []
    order = order.split(', ')
    for o in order:
        match = ''
        amount = 0
        for p in PRODUCTS:
            if o.endswith(p):
                if len(p) > len(match):
                    match = p
                try:
                    amount_str = o.removesuffix(p).strip(' ')
                    amount = int(amount_str)
                except ValueError:
                    return f'Ik kan het aantal niet achterhalen in \"{o}\"', None
                except Exception as e:
                    raise e
                break
        if match == '':
            return f'Ik begrijp niet wat je bedoelt met {o}', None
        else:
            matches.append(match)
            amounts.append(amount)
    return matches

matches = order_parser(order)
print(matches)