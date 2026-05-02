import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface Quote {
  content: string;
  author: string;
  tags: string[];
}

const QUOTES: Quote[] = [
  { content: 'Success is not the key to happiness. Happiness is the key to success.', author: 'Albert Schweitzer', tags: ['motivational'] },
  { content: 'The only way to do great work is to love what you do.', author: 'Steve Jobs', tags: ['motivational'] },
  { content: 'It does not matter how slowly you go as long as you do not stop.', author: 'Confucius', tags: ['motivational'] },
  { content: 'Believe you can and you\'re halfway there.', author: 'Theodore Roosevelt', tags: ['motivational'] },
  { content: 'Your time is limited, don\'t waste it living someone else\'s life.', author: 'Steve Jobs', tags: ['motivational'] },
  { content: 'The future belongs to those who believe in the beauty of their dreams.', author: 'Eleanor Roosevelt', tags: ['motivational'] },
  { content: 'Hard work beats talent when talent doesn\'t work hard.', author: 'Tim Notke', tags: ['motivational'] },
  { content: 'Don\'t watch the clock; do what it does. Keep going.', author: 'Sam Levenson', tags: ['motivational'] },
  { content: 'Opportunities don\'t happen. You create them.', author: 'Chris Grosser', tags: ['motivational'] },
  { content: 'Dream big and dare to fail.', author: 'Norman Vaughan', tags: ['motivational'] },
];

@Injectable({
  providedIn: 'root'
})
export class QuoteService {

  getRandomQuote(): Observable<Quote> {
    return of(QUOTES[Math.floor(Math.random() * QUOTES.length)]);
  }

  getQuotes(limit: number = 5): Observable<Quote[]> {
    return of(QUOTES.slice(0, limit));
  }
}
