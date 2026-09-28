import smtplib
import sys

def test_smtp():
    try:
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.set_debuglevel(1)
        server.ehlo()
        server.starttls()
        server.login('otabeksotimov9@gmail.com', 'stakshkazvdcxnfo')
        print("Login SUCCESSFUL")
        server.quit()
    except Exception as e:
        print("Login FAILED:", e)

if __name__ == "__main__":
    test_smtp()
