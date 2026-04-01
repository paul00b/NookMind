import { describe, expect, it } from 'vitest';
import { pickByMediaMode, renderByMediaMode } from './mediaMode';

describe('media mode helpers', () => {
  const options = {
    books: 'books-view',
    movies: 'movies-view',
    series: 'series-view',
  };

  it('selects the matching value for each mode', () => {
    expect(pickByMediaMode('books', options)).toBe('books-view');
    expect(pickByMediaMode('movies', options)).toBe('movies-view');
    expect(pickByMediaMode('series', options)).toBe('series-view');
  });

  it('renders the same node selected for the active mode', () => {
    const rendered = renderByMediaMode('movies', {
      books: <div>books</div>,
      movies: <div>movies</div>,
      series: <div>series</div>,
    });

    expect(rendered).toMatchObject({
      props: { children: 'movies' },
    });
  });
});
